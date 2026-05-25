package net.busybee.clearlaggenhanced.managers;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.updater.ConfigMigrator;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        try {
            ConfigMigrator migrator = new ConfigMigrator(plugin);
            config = migrator.migrate("config.yml");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to migrate config.yml: " + e.getMessage());
            e.printStackTrace();
            config = null;
        }

        if (config == null) {
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            config = plugin.getConfig();
        }
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
    public void set(@NotNull String path, @NotNull Object value) {
        config.set(path, value);
    }
    public boolean contains(@NotNull String path) {
        return config != null && config.contains(path);
    }

    public void save() {
        if (config == null) {
            return;
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config.yml: " + e.getMessage());
        }
    }
}
