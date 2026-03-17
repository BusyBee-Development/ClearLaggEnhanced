package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.utils.ConfigMigrator;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ConfigManager {

    private final ClearLaggEnhanced plugin;
    @Getter private FileConfiguration config;

    public ConfigManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        // Use ConfigMigrator to automatically update config with new keys
        ConfigMigrator migrator = new ConfigMigrator(plugin);
        config = migrator.migrate("config.yml");

        // Fallback to standard reload if migration failed
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
    public double getDouble(@NotNull String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    public String getString(@NotNull String path, @NotNull String defaultValue) {
        return config.getString(path, defaultValue);
    }

    public List<String> getStringList(@NotNull String path) {
        return config.getStringList(path);
    }

    public List<Integer> getIntegerList(@NotNull String path) {
        return config.getIntegerList(path);
    }

    public Map<String, Object> getConfigSection(@NotNull String path) {
        if (config.isConfigurationSection(path)) {
            Map<String, Object> result = new HashMap<>();
            ConfigurationSection section = config.getConfigurationSection(path);
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    result.put(key, section.get(key));
                }
            }

            return result;
        }

        return null;
    }

    public void set(@NotNull String path, @NotNull Object value) {
        config.set(path, value);
    }

    public void save() {
        plugin.saveConfig();
    }
}
