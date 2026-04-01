package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.utils.ConfigMigrator;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

<<<<<<< HEAD
=======
import java.io.File;
import java.io.IOException;
>>>>>>> dev
import java.util.*;

public class ConfigManager {

    private final ClearLaggEnhanced plugin;
<<<<<<< HEAD
=======
    private final File configFile;
>>>>>>> dev
    @Getter private FileConfiguration config;

    public ConfigManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
<<<<<<< HEAD
=======
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
>>>>>>> dev
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
<<<<<<< HEAD
    public void save() {
        plugin.saveConfig();
=======

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
>>>>>>> dev
    }
}
