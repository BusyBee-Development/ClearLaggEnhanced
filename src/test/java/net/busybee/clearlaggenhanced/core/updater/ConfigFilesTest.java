package net.busybee.clearlaggenhanced.core.updater;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.busybee.clearlaggenhanced.core.updater.TestPlugin.TEST_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFilesTest {

    @TempDir
    Path dataFolder;

    private Plugin plugin;
    private String customDefaults = "interval: 300\nwhitelist:\n  - \"VILLAGER\"\n";

    @BeforeEach
    void setUp() {
        plugin = TestPlugin.create(dataFolder, () -> customDefaults);
    }

    @Test
    void createsAMissingFileFromThePackagedDefault() throws Exception {
        File file = dataFolder.resolve(TEST_PATH).toFile();

        ConfigFiles.Loaded loaded = ConfigFiles.load(plugin, TEST_PATH, file);

        assertTrue(loaded.valid());
        assertEquals(customDefaults, Files.readString(file.toPath()));
        assertEquals(300, loaded.config().getInt("interval"));
    }

    @Test
    void invalidYamlIsReportedAndNeverReplaced() throws Exception {
        // The old updater deleted a file like this and regenerated defaults, losing the owner's whitelist.
        String broken = "interval: 600\nwhitelist:\n- COW\n- WOLF\nmessage: &cBroken: value\n  - nope\n";
        File file = write(TEST_PATH, broken);

        ConfigFiles.Loaded loaded = ConfigFiles.load(plugin, TEST_PATH, file);

        assertFalse(loaded.valid());
        assertEquals(broken, Files.readString(file.toPath()));
        assertFalse(Files.exists(file.toPath().resolveSibling("backups")));
        assertEquals(false, ConfigFiles.unreadableFiles().get(TEST_PATH));
    }

    @Test
    void aMistakeFallsBackToTheLastWorkingCopy() throws Exception {
        String working = "interval: 600\nwhitelist:\n- COW\n- WOLF\n";
        File file = write(TEST_PATH, working);
        assertTrue(ConfigFiles.load(plugin, TEST_PATH, file).valid());

        String broken = working + "message: &cBroken: value\n  - nope\n";
        Files.writeString(file.toPath(), broken);
        ConfigFiles.Loaded loaded = ConfigFiles.load(plugin, TEST_PATH, file);

        // The owner's own settings keep running, not the packaged defaults.
        assertTrue(loaded.valid());
        assertEquals(600, loaded.config().getInt("interval"));
        assertEquals(List.of("COW", "WOLF"), loaded.config().getStringList("whitelist"));
        assertEquals(broken, Files.readString(file.toPath()));
        assertEquals(true, ConfigFiles.unreadableFiles().get(TEST_PATH));

        Files.writeString(file.toPath(), working);
        assertTrue(ConfigFiles.load(plugin, TEST_PATH, file).valid());
        assertFalse(ConfigFiles.unreadableFiles().containsKey(TEST_PATH));
    }

    @Test
    void menuChangesAreKeptInTheWorkingCopy() throws Exception {
        File file = write(TEST_PATH, "interval: 300\nwhitelist:\n- COW\n");
        ConfigFiles.load(plugin, TEST_PATH, file);
        assertTrue(ConfigFiles.setValue(plugin, file, "whitelist", List.of("COW", "WOLF")));

        Files.writeString(file.toPath(), Files.readString(file.toPath()) + "message: &cBroken: value\n  - nope\n");
        ConfigFiles.Loaded loaded = ConfigFiles.load(plugin, TEST_PATH, file);

        assertEquals(List.of("COW", "WOLF"), loaded.config().getStringList("whitelist"));
    }

    @Test
    void setValueChangesOnlyThatKey() throws Exception {
        File file = write(TEST_PATH, """
                # How often to clear
                interval: 300 # seconds
                protect-named-entities: true
                message: "<red>Clearing: now"
                """);

        assertTrue(ConfigFiles.setValue(plugin, file, "interval", 600));
        assertTrue(ConfigFiles.setValue(plugin, file, "protect-named-entities", false));

        assertEquals("""
                # How often to clear
                interval: 600 # seconds
                protect-named-entities: false
                message: "<red>Clearing: now"
                """, Files.readString(file.toPath()));
    }

    @Test
    void setValueKeepsTheOwnersListLayout() throws Exception {
        File file = write(TEST_PATH, """
                # Entities that should NOT be cleared.
                whitelist:
                - "VILLAGER"
                - "COW"

                # Items
                item-whitelist:
                  - ELYTRA
                worlds: []
                """);

        assertTrue(ConfigFiles.setValue(plugin, file, "whitelist", List.of("VILLAGER", "COW", "WOLF")));
        assertTrue(ConfigFiles.setValue(plugin, file, "item-whitelist", List.of("ELYTRA", "MACE")));
        assertTrue(ConfigFiles.setValue(plugin, file, "worlds", List.of("world")));

        assertEquals("""
                # Entities that should NOT be cleared.
                whitelist:
                - "VILLAGER"
                - "COW"
                - "WOLF"

                # Items
                item-whitelist:
                  - ELYTRA
                  - MACE
                worlds:
                  - world
                """, Files.readString(file.toPath()));
    }

    @Test
    void setValueWritesAListOfSections() throws Exception {
        File file = write(TEST_PATH, """
                adaptive-interval:
                  enabled: true
                  tiers:
                    - threshold: 0
                      interval: 900
                    - threshold: 5000
                      interval: 750
                # after
                interval: 300
                """);
        Map<String, Object> tier = new LinkedHashMap<>();
        tier.put("threshold", 100);
        tier.put("interval", 60);

        assertTrue(ConfigFiles.setValue(plugin, file, "adaptive-interval.tiers", List.of(tier)));

        assertEquals("""
                adaptive-interval:
                  enabled: true
                  tiers:
                    - threshold: 100
                      interval: 60
                # after
                interval: 300
                """, Files.readString(file.toPath()));
    }

    @Test
    void setValueRefusesToSaveInvalidYaml() throws Exception {
        String broken = "interval: 300\nmessage: &cBroken: value\n";
        File file = write(TEST_PATH, broken);

        assertFalse(ConfigFiles.setValue(plugin, file, "interval", 600));
        assertEquals(broken, Files.readString(file.toPath()));
    }

    @Test
    void removeKeyTakesOutOnlyThatKeyAndItsComment() throws Exception {
        File file = write(TEST_PATH, """
                # Old toggle
                enabled: true

                # How often to clear
                interval: 300
                """);

        assertTrue(ConfigFiles.removeKey(plugin, file, "enabled"));

        assertEquals("""

                # How often to clear
                interval: 300
                """, Files.readString(file.toPath()));
    }

    private File write(String relativePath, String contents) throws IOException {
        Path path = dataFolder.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents);
        return path.toFile();
    }
}
