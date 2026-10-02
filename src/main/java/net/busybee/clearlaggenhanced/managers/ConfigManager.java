package net.busybee.clearlaggenhanced.managers;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.updater.ConfigFiles;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ConfigManager {

    private final ClearLaggEnhanced plugin;
    private final File configFile;
    @Getter private FileConfiguration config;

    public ConfigManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.reload();
    }

    public void reload() {
        ConfigFiles.Loaded loaded = ConfigFiles.load(plugin, "config.yml", configFile);
        if (!loaded.valid()) {
            plugin.getLogger().severe("config.yml has no earlier working copy to fall back on, so the default module toggles and database settings are used until it is fixed.");
        }
        config = loaded.config();
    }

    public boolean getBoolean(@NotNull String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    public boolean getBoolean(@NotNull String path) {
        return config.getBoolean(path);
    }
    public int getInt(@NotNull String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    // Changes one setting and writes only that key to config.yml, leaving the rest of the owner's file as it is.
    public void setValue(@NotNull String path, @NotNull Object value) {
        config.set(path, value);
        ConfigFiles.setValue(plugin, configFile, path, value);
    }
    public boolean contains(@NotNull String path) {
        return config != null && config.contains(path);
    }
}
