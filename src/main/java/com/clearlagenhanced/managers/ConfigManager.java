package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {

    private static final int CURRENT_CONFIG_VERSION = 1;

    private final ClearLaggEnhanced plugin;
    @Getter private FileConfiguration config;
    private boolean debuggingInProgress = false;

    public ConfigManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        checkConfigVersion();
        debugLog("Configuration reloaded");
    }

    private void checkConfigVersion() {
        int configVersion = config.getInt("config-version", 0);

        if (configVersion == 0) {
            plugin.getLogger().warning("No config version found! Updating config...");
            migrateConfig(0);
        } else if (configVersion < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().info("Config version " + configVersion + " is outdated. Updating to version " + CURRENT_CONFIG_VERSION + "...");
            migrateConfig(configVersion);
        } else if (configVersion > CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("Config version " + configVersion + " is newer than supported version " + CURRENT_CONFIG_VERSION + "!");
            plugin.getLogger().warning("Please update the plugin or reset your config.");
        }
    }

    private void migrateConfig(int fromVersion) {
        try {
            // Backup the old config
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup-v" + fromVersion);

            if (configFile.exists()) {
                YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
                oldConfig.save(backupFile);
                plugin.getLogger().info("Backed up old config to: " + backupFile.getName());
            }

            // Save new default config
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            FileConfiguration newConfig = plugin.getConfig();

            // Migrate settings from old config to new config
            if (configFile.exists() && backupFile.exists()) {
                YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(backupFile);
                migrateSettings(oldConfig, newConfig);
                plugin.saveConfig();
            }

            // Update version
            config.set("config-version", CURRENT_CONFIG_VERSION);
            plugin.saveConfig();

            plugin.getLogger().info("Config successfully updated to version " + CURRENT_CONFIG_VERSION);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to migrate config: " + e.getMessage());
        }
    }

    private void migrateSettings(@NotNull FileConfiguration oldConfig, @NotNull FileConfiguration newConfig) {
        // Migrate all user-set values from old config to new config
        // This preserves user customizations while adding new default values

        for (String key : oldConfig.getKeys(true)) {
            // Skip the version key itself
            if (key.equals("config-version")) {
                continue;
            }

            // Only migrate if the key exists in new config (to avoid deprecated settings)
            if (newConfig.contains(key) && !oldConfig.isConfigurationSection(key)) {
                Object value = oldConfig.get(key);
                newConfig.set(key, value);
                plugin.getLogger().info("Migrated setting: " + key + " = " + value);
            }
        }
    }

    private void debugLog(@NotNull String message) {
        if (debuggingInProgress) {
            return;
        }

        debuggingInProgress = true;
        try {
            if (config.getBoolean("debug", false)) {
                plugin.getLogger().info("[DEBUG] ConfigManager: " + message);
            }
        } finally {
            debuggingInProgress = false;
        }
    }

    public boolean getBoolean(@NotNull String path, boolean defaultValue) {
        boolean value = config.getBoolean(path, defaultValue);
        debugLog("Getting boolean " + path + " = " + value);
        return value;
    }

    public boolean getBoolean(@NotNull String path) {
        boolean value = config.getBoolean(path);
        debugLog("Getting boolean " + path + " = " + value);
        return value;
    }

    public int getInt(@NotNull String path, int defaultValue) {
        int value = config.getInt(path, defaultValue);
        debugLog("Getting int " + path + " = " + value);
        return value;
    }

    public int getInt(@NotNull String path) {
        int value = config.getInt(path);
        debugLog("Getting int " + path + " = " + value);
        return value;
    }

    public double getDouble(@NotNull String path, double defaultValue) {
        double value = config.getDouble(path, defaultValue);
        debugLog("Getting double " + path + " = " + value);
        return value;
    }

    public double getDouble(@NotNull String path) {
        double value = config.getDouble(path);
        debugLog("Getting double " + path + " = " + value);
        return value;
    }

    public String getString(@NotNull String path, @NotNull String defaultValue) {
        String value = config.getString(path, defaultValue);
        debugLog("Getting string " + path + " = " + value);
        return value;
    }

    public String getString(@NotNull String path) {
        String value = config.getString(path);
        debugLog("Getting string " + path + " = " + value);
        return value;
    }

    public List<String> getStringList(@NotNull String path) {
        List<String> value = config.getStringList(path);
        debugLog("Getting string list " + path + " = " + value);
        return value;
    }

    public List<Integer> getIntegerList(@NotNull String path) {
        List<Integer> value = config.getIntegerList(path);
        debugLog("Getting integer list " + path + " = " + value);
        return value;
    }

    public void set(@NotNull String path, @NotNull Object value) {
        debugLog("Setting " + path + " = " + value);
        config.set(path, value);

        try {
            plugin.saveConfig();
            debugLog("Configuration saved successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save configuration: " + e.getMessage());
        }
    }
}
