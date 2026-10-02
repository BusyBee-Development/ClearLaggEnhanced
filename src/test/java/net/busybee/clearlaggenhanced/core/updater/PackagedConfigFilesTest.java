package net.busybee.clearlaggenhanced.core.updater;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Catches mistakes in the packaged config files before they reach a server. */
class PackagedConfigFilesTest {

    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Pattern VALUE = Pattern.compile("^\\s*(?:- |[\\w.-]+:\\s+)(.+)$");
    private static final Pattern NON_TEXT = Pattern.compile("^(?:true|false|-?\\d+(?:\\.\\d+)?|\\[]|\\{})(?:\\s+#.*)?$");

    @Test
    void everyFileParses() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path file : packagedFiles()) {
            YamlConfiguration yaml = new YamlConfiguration();
            try {
                yaml.loadFromString(Files.readString(file));
            } catch (Exception e) {
                problems.add(name(file) + ": " + ConfigMigrator.describeCause(e));
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void messageTextIsQuoted() throws IOException {
        List<String> problems = new ArrayList<>();
        for (Path file : packagedFiles()) {
            if (!file.getFileName().toString().equals("messages.yml")) continue;
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                Matcher matcher = VALUE.matcher(line);
                if (!matcher.matches()) continue;
                String value = matcher.group(1).strip();
                if ("\"'|>".indexOf(value.charAt(0)) >= 0 || NON_TEXT.matcher(value).matches()) continue;
                problems.add(name(file) + ":" + (i + 1) + ": " + line.strip());
            }
        }
        assertEquals(List.of(), problems, "Message text must be in quotes");
    }

    @Test
    void ownerCollectionsExistInTheirPackagedFiles() throws Exception {
        for (Path file : packagedFiles()) {
            String relativePath = name(file);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(Files.readString(file));
            for (String path : ConfigMigrator.ownerCollections(relativePath)) {
                assertTrue(yaml.isConfigurationSection(path), relativePath + " has no section " + path);
            }
        }
    }

    private static List<Path> packagedFiles() throws IOException {
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            return files.filter(file -> file.toString().endsWith(".yml"))
                    .filter(file -> !file.getFileName().toString().equals("plugin.yml"))
                    .sorted()
                    .toList();
        }
    }

    private static String name(Path file) {
        return RESOURCES.relativize(file).toString().replace('\\', '/');
    }
}
