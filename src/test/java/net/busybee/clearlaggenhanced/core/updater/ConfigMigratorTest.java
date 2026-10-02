package net.busybee.clearlaggenhanced.core.updater;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static net.busybee.clearlaggenhanced.core.updater.TestPlugin.TEST_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMigratorTest {

    @TempDir
    Path dataFolder;

    private ConfigMigrator migrator;
    private String customDefaults;

    @BeforeEach
    void setUp() {
        migrator = new ConfigMigrator(TestPlugin.create(dataFolder, () -> customDefaults));
    }

    @Test
    void insertsMissingKeysWithoutTouchingOwnerText() throws Exception {
        customDefaults = """
                # Header

                # Chat prefix
                prefix: "<gray>[Server]"

                settings:
                  enabled: true
                  # How long it lasts
                  duration: 30
                  message: "<red>Hi"

                # Brand new section
                extra:
                  mode: "fast"
                """;
        String owner = """
                # Header

                # Chat prefix
                prefix: "&7[My Server]"   # my own comment

                settings:
                    enabled: false
                    message: '<green>Hello: world'
                """;
        File file = write(TEST_PATH, owner);

        assertTrue(migrator.migrate(TEST_PATH, file));

        assertEquals("""
                # Header

                # Chat prefix
                prefix: "&7[My Server]"   # my own comment

                settings:
                    enabled: false
                    # How long it lasts
                    duration: 30
                    message: '<green>Hello: world'

                # Brand new section
                extra:
                  mode: "fast"
                """, Files.readString(file.toPath()));
        assertTrue(backupExists(file));
    }

    @Test
    void keepsWindowsLineEndings() throws Exception {
        customDefaults = "first: \"a\"\nsecond: \"b\"\n";
        File file = write(TEST_PATH, "first: \"mine\"\r\n");

        assertTrue(migrator.migrate(TEST_PATH, file));

        assertEquals("first: \"mine\"\r\nsecond: \"b\"\r\n", Files.readString(file.toPath()));
    }

    @Test
    void addsNewSettingInsideFeatureGroup() throws Exception {
        customDefaults = """
                settings:
                  first:
                    enabled: true
                  second:
                    enabled: true
                  third:
                    enabled: false
                """;
        File file = write(TEST_PATH, """
                settings:
                  first:
                    enabled: false
                  second:
                    enabled: true
                """);

        assertTrue(migrator.migrate(TEST_PATH, file));

        YamlConfiguration result = YamlConfiguration.loadConfiguration(file);
        assertFalse(result.getBoolean("settings.first.enabled"));
        assertTrue(result.contains("settings.third.enabled"));
    }

    @Test
    void handlesListsWrittenAtTheKeysIndent() throws Exception {
        customDefaults = """
                names:
                  - a
                  - b
                after: 1
                """;
        File file = write(TEST_PATH, """
                names:
                - x
                """);

        assertTrue(migrator.migrate(TEST_PATH, file));

        YamlConfiguration result = YamlConfiguration.loadConfiguration(file);
        assertEquals(List.of("x"), result.getStringList("names"));
        assertEquals(1, result.getInt("after"));
    }

    @Test
    void doesNotPutBackLimitsTheOwnerRemoved() throws Exception {
        // Deleting a cap is how an owner lifts it, so it must not come back on the next start.
        assertLeftAlone("module/mob-limiter/config.yml", "ZOMBIE:");
        assertLeftAlone("module/misc-entity-limiter/config.yml", "PAINTING:");
    }

    @Test
    void addsNothingInsideAnOptionalOverride() throws Exception {
        // Fields omitted from notifications.clear-complete fall back to the main notification settings.
        assertLeftAlone("module/entity-clearing/config.yml", "name: \"ENTITY_PLAYER_LEVELUP\"");
    }

    @Test
    void upToDateFileIsNotRewritten() throws Exception {
        for (String path : packagedFiles()) {
            File file = write(path, resource(path));
            assertFalse(migrator.migrate(path, file), path);
            assertEquals(resource(path), Files.readString(file.toPath()), path);
        }
    }

    @Test
    void everyPackagedFileCanBeRebuiltFromAnEmptyFile() throws Exception {
        for (String path : packagedFiles()) {
            File file = write(path, "");
            assertTrue(migrator.migrate(path, file), path);
            YamlConfiguration expected = new YamlConfiguration();
            expected.loadFromString(resource(path));
            YamlConfiguration result = YamlConfiguration.loadConfiguration(file);
            assertEquals(expected.getKeys(true), result.getKeys(true), path);
        }
    }

    @Test
    void filesSavedByBukkitGetRemovedSettingsBack() throws Exception {
        for (String path : packagedFiles()) {
            YamlConfiguration expected = new YamlConfiguration();
            expected.loadFromString(resource(path));
            YamlConfiguration owner = new YamlConfiguration();
            owner.loadFromString(resource(path));
            boolean remove = false;
            for (String key : expected.getKeys(true)) {
                if (expected.isConfigurationSection(key) || insideOwnerCollection(path, key)) continue;
                if (remove) owner.set(key, null);
                remove = !remove;
            }
            File file = write(path, owner.saveToString());

            migrator.migrate(path, file);

            YamlConfiguration result = YamlConfiguration.loadConfiguration(file);
            for (String key : expected.getKeys(true)) {
                if (insideOwnerCollection(path, key)) continue;
                assertEquals(expected.get(key) instanceof ConfigurationSection, result.isConfigurationSection(key), path + ": " + key);
                if (!expected.isConfigurationSection(key)) assertEquals(expected.get(key), result.get(key), path + ": " + key);
            }
        }
    }

    @Test
    void invalidYamlIsLeftUntouched() throws Exception {
        String path = "module/entity-clearing/config.yml";
        String broken = "whitelist:\n- VILLAGER\ninterval: &cBroken: value\n  - nope\n";
        File file = write(path, broken);

        assertFalse(migrator.migrate(path, file));
        assertEquals(broken, Files.readString(file.toPath()));
        assertFalse(backupExists(file));
    }

    private void assertLeftAlone(String path, String removedLine) throws Exception {
        String owner = withoutLines(resource(path), removedLine);
        assertNotEquals(resource(path), owner, path);
        File file = write(path, owner);

        assertFalse(migrator.migrate(path, file), path);
        assertEquals(owner, Files.readString(file.toPath()), path);
    }

    private static boolean insideOwnerCollection(String path, String key) {
        return ConfigMigrator.ownerCollections(path).stream().anyMatch(collection -> key.startsWith(collection + "."));
    }

    private static String withoutLines(String text, String startingWith) {
        return text.lines()
                .filter(line -> !line.strip().startsWith(startingWith))
                .reduce("", (joined, line) -> joined + line + "\n");
    }

    private File write(String relativePath, String contents) throws IOException {
        Path path = dataFolder.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents);
        return path.toFile();
    }

    private static boolean backupExists(File file) throws IOException {
        Path backups = file.toPath().resolveSibling("backups");
        if (!Files.isDirectory(backups)) return false;
        try (var files = Files.list(backups)) {
            return files.anyMatch(backup -> backup.getFileName().toString().startsWith(file.getName() + ".backup-"));
        }
    }

    private static List<String> packagedFiles() throws IOException {
        Path resources = Path.of("src/main/resources");
        try (var files = Files.walk(resources)) {
            return files.filter(file -> file.toString().endsWith(".yml"))
                    .filter(file -> !file.getFileName().toString().equals("plugin.yml"))
                    .map(file -> resources.relativize(file).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }

    private static String resource(String relativePath) throws IOException {
        try (InputStream stream = ConfigMigratorTest.class.getResourceAsStream("/" + relativePath)) {
            assertNotNull(stream, relativePath);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
