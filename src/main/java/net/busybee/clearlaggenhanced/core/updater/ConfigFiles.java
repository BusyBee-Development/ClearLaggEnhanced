package net.busybee.clearlaggenhanced.core.updater;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Reads and writes the plugin's YAML files without ever replacing what the owner wrote: a file that
 * fails to parse is reported and left alone, and saving a setting edits only that key's lines.
 *
 * <p>Every file that loads is copied to {@code backups/<name>.last-good} next to it. A file that
 * later fails to parse runs on that copy, so a typo costs the owner their latest edit and nothing else.
 */
public final class ConfigFiles {

    private static final Map<String, Boolean> UNREADABLE = new ConcurrentHashMap<>();

    /**
     * @param valid false when neither the file nor a last working copy of it could be read, in which
     *              case {@code config} holds the packaged defaults and nothing destructive should run off it
     * @param onWorkingCopy true when the file itself could not be read and {@code config} is its last working copy
     */
    public record Loaded(@NotNull FileConfiguration config, boolean valid, boolean onWorkingCopy) {
    }

    private ConfigFiles() {
    }

    /**
     * Creates the file from the packaged default if it is missing, adds settings a newer version
     * introduced, then loads it.
     *
     * @param resourcePath path of the packaged default inside the jar, e.g. {@code module/afk/config.yml}
     */
    public static @NotNull Loaded load(@NotNull Plugin plugin, @NotNull String resourcePath, @NotNull File file) {
        boolean packaged = hasResource(plugin, resourcePath);

        if (!file.exists() && packaged) {
            copyDefault(plugin, resourcePath, file);
        }
        if (packaged) {
            new ConfigMigrator(plugin).migrate(resourcePath, file);
        }

        YamlConfiguration config = new YamlConfiguration();
        if (!file.exists()) {
            return new Loaded(config, true, false);
        }

        String problem;
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            config.loadFromString(text);
            rememberWorkingCopy(plugin, file, text);
            UNREADABLE.remove(resourcePath);
            return new Loaded(config, true, false);
        } catch (InvalidConfigurationException e) {
            problem = resourcePath + " is not valid YAML and was NOT loaded. The file was left as it is; fix it and run /lagg reload: "
                    + ConfigMigrator.describeCause(e) + " " + ConfigMigrator.QUOTE_HINT;
        } catch (IOException e) {
            problem = resourcePath + " could not be read and was NOT loaded: " + ConfigMigrator.describeCause(e);
        }
        plugin.getLogger().severe(problem);

        // One mistake should not switch anything off: carry on with the owner's settings as they
        // were the last time this file loaded.
        YamlConfiguration workingCopy = loadWorkingCopy(file);
        if (workingCopy != null) {
            plugin.getLogger().severe("Until then, " + resourcePath + " runs on its last working copy (backups/"
                    + workingCopyOf(file).getName() + "), so your latest edits to it are not in effect.");
            UNREADABLE.put(resourcePath, true);
            return new Loaded(workingCopy, true, true);
        }

        UNREADABLE.put(resourcePath, false);
        return new Loaded(loadPackagedDefaults(plugin, resourcePath), false, false);
    }

    /**
     * The files that failed to load, by packaged path. The value is true if the file is running on
     * its last working copy, false if there was none to fall back on.
     */
    public static @NotNull Map<String, Boolean> unreadableFiles() {
        return new TreeMap<>(UNREADABLE);
    }

    private static File workingCopyOf(File file) {
        return new File(new File(file.getParentFile(), "backups"), file.getName() + ".last-good");
    }

    // Keeps the text of a file that parsed, so a later mistake in it has something to fall back on.
    private static void rememberWorkingCopy(Plugin plugin, File file, String text) {
        File copy = workingCopyOf(file);
        try {
            if (copy.exists() && Files.readString(copy.toPath(), StandardCharsets.UTF_8).equals(text)) return;
            copy.getParentFile().mkdirs();
            Files.writeString(copy.toPath(), text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save the working copy of " + file.getName(), e);
        }
    }

    private static YamlConfiguration loadWorkingCopy(File file) {
        File copy = workingCopyOf(file);
        if (!copy.exists()) return null;

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(copy);
            return config;
        } catch (IOException | InvalidConfigurationException e) {
            return null;
        }
    }

    /**
     * Saves one value into a file, keeping everything else in it. If the file is not valid YAML the
     * change is not written, so a typo elsewhere in the file can never cost the owner the rest of
     * their settings.
     *
     * @return true if the value was saved
     */
    public static boolean setValue(@NotNull Plugin plugin, @NotNull File file, @NotNull String key, @NotNull Object value) {
        String text;
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            text = file.exists() ? Files.readString(file.toPath(), StandardCharsets.UTF_8) : "";
            yaml.loadFromString(text);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("Could not save " + key + " to " + file.getName()
                    + " because the file is not valid YAML; the change lasts until the next restart or reload. "
                    + ConfigMigrator.describeCause(e) + " " + ConfigMigrator.QUOTE_HINT);
            return false;
        }

        // Edit just that key's lines so the rest of the file keeps the owner's quotes and comments;
        // fall back to a normal save if the key isn't there yet or the edit didn't read back right.
        yaml.set(key, value);
        Object expected = yaml.get(key);
        String updated = null;
        YamlText doc = new YamlText(text);
        if (doc.setValue(key, value)) {
            updated = doc.render();
            YamlConfiguration check = new YamlConfiguration();
            try {
                check.loadFromString(updated);
                if (!Objects.equals(check.get(key), expected)) updated = null;
            } catch (InvalidConfigurationException e) {
                updated = null;
            }
        }
        if (updated == null) {
            updated = yaml.saveToString();
        }

        return write(plugin, file, updated, key);
    }

    /**
     * Deletes one key from a file, keeping everything else in it. Does nothing if the file is not
     * valid YAML or the key cannot be taken out without disturbing other settings.
     *
     * @return true if the key was removed
     */
    public static boolean removeKey(@NotNull Plugin plugin, @NotNull File file, @NotNull String key) {
        String text;
        YamlConfiguration before = new YamlConfiguration();
        try {
            text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            before.loadFromString(text);
        } catch (IOException | InvalidConfigurationException e) {
            return false;
        }

        YamlText doc = new YamlText(text);
        if (!doc.remove(key)) return false;

        String updated = doc.render();
        YamlConfiguration after = new YamlConfiguration();
        try {
            after.loadFromString(updated);
        } catch (InvalidConfigurationException e) {
            return false;
        }
        before.set(key, null);
        if (!before.getKeys(true).equals(after.getKeys(true))) return false;
        for (String path : before.getKeys(true)) {
            if (before.isConfigurationSection(path)) continue;
            if (!Objects.equals(before.get(path), after.get(path))) return false;
        }

        return write(plugin, file, updated, key);
    }

    private static boolean write(Plugin plugin, File file, String text, String key) {
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
            // Menu changes count as working settings too, or a later fallback would lose them.
            rememberWorkingCopy(plugin, file, text);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save " + key + " to " + file.getName(), e);
            return false;
        }
    }

    private static boolean hasResource(Plugin plugin, String resourcePath) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            return stream != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static void copyDefault(Plugin plugin, String resourcePath, File file) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) return;
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.copy(stream, file.toPath());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not create default " + resourcePath, e);
        }
    }

    private static YamlConfiguration loadPackagedDefaults(Plugin plugin, String resourcePath) {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) return new YamlConfiguration();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read packaged defaults for: " + resourcePath, e);
            return new YamlConfiguration();
        }
    }
}
