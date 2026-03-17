package com.clearlagenhanced.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Intelligent configuration migration utility that automatically updates YAML files
 * by merging new defaults while preserving user customizations and comments.
 */
public class ConfigMigrator {

    private final Plugin plugin;
    private final List<String> addedKeys = new ArrayList<>();
    private final List<String> removedKeys = new ArrayList<>();

    public ConfigMigrator(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Migrates a configuration file by merging it with the default from the plugin JAR.
     *
     * @param fileName The name of the config file (e.g., "config.yml", "messages.yml")
     * @return The migrated FileConfiguration, or null if migration failed
     */
    public FileConfiguration migrate(@NotNull String fileName) {
        addedKeys.clear();
        removedKeys.clear();

        File configFile = new File(plugin.getDataFolder(), fileName);

        // If config doesn't exist, just save the default
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info("Created new " + fileName + " from defaults");
            return YamlConfiguration.loadConfiguration(configFile);
        }

        // Load user's existing config
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

        // Load default config from JAR
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream == null) {
            plugin.getLogger().warning("Could not find default " + fileName + " in plugin JAR!");
            return userConfig;
        }

        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
        );

        // Check if migration is needed
        if (!needsMigration(userConfig, defaultConfig)) {
            plugin.getLogger().info(fileName + " is already up to date");
            return userConfig;
        }

        // Create backup before migration
        createBackup(configFile);

        // Perform the smart merge
        mergeConfigs(userConfig, defaultConfig, "");

        // Remove obsolete keys
        removeObsoleteKeys(userConfig, defaultConfig, "");

        // Save the migrated config
        try {
            userConfig.save(configFile);
            logMigrationSummary(fileName);
            return userConfig;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save migrated " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if migration is needed by comparing keys between user and default configs.
     */
    private boolean needsMigration(@NotNull FileConfiguration userConfig, @NotNull FileConfiguration defaultConfig) {
        Set<String> userKeys = getAllKeys(userConfig, "");
        Set<String> defaultKeys = getAllKeys(defaultConfig, "");

        // Check for new keys in default
        for (String key : defaultKeys) {
            if (!userKeys.contains(key)) {
                return true;
            }
        }

        // Check for obsolete keys in user config
        for (String key : userKeys) {
            if (!defaultKeys.contains(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Recursively gets all keys in a configuration section.
     */
    private Set<String> getAllKeys(@NotNull ConfigurationSection section, @NotNull String prefix) {
        Set<String> keys = new HashSet<>();
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            keys.add(fullKey);

            if (section.isConfigurationSection(key)) {
                ConfigurationSection subSection = section.getConfigurationSection(key);
                if (subSection != null) {
                    keys.addAll(getAllKeys(subSection, fullKey));
                }
            }
        }
        return keys;
    }

    /**
     * Recursively merges default values into the user config, preserving user's existing values.
     * New sections and keys from the default config will be added with their comments.
     */
    private void mergeConfigs(@NotNull ConfigurationSection userConfig,
                              @NotNull ConfigurationSection defaultConfig,
                              @NotNull String path) {
        for (String key : defaultConfig.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (!userConfig.contains(key)) {
                // New key - add it with default value and comments
                userConfig.set(key, defaultConfig.get(key));
                userConfig.setComments(key, defaultConfig.getComments(key));
                userConfig.setInlineComments(key, defaultConfig.getInlineComments(key));
                addedKeys.add(fullPath);
            } else if (defaultConfig.isConfigurationSection(key)) {
                // Recursively merge sections
                ConfigurationSection userSection = userConfig.getConfigurationSection(key);
                ConfigurationSection defaultSection = defaultConfig.getConfigurationSection(key);
                if (userSection != null && defaultSection != null) {
                    mergeConfigs(userSection, defaultSection, fullPath);
                }
            }
            // If key exists and is not a section, preserve user's value (no action needed)
        }
    }

    /**
     * Removes keys from user config that no longer exist in the default config.
     */
    private void removeObsoleteKeys(@NotNull ConfigurationSection userConfig,
                                    @NotNull ConfigurationSection defaultConfig,
                                    @NotNull String path) {
        Set<String> userKeys = new HashSet<>(userConfig.getKeys(false));

        for (String key : userKeys) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (!defaultConfig.contains(key)) {
                // Obsolete key - remove it
                userConfig.set(key, null);
                removedKeys.add(fullPath);
            } else if (userConfig.isConfigurationSection(key) && defaultConfig.isConfigurationSection(key)) {
                // Recursively check sections
                ConfigurationSection userSection = userConfig.getConfigurationSection(key);
                ConfigurationSection defaultSection = defaultConfig.getConfigurationSection(key);
                if (userSection != null && defaultSection != null) {
                    removeObsoleteKeys(userSection, defaultSection, fullPath);
                }
            }
        }
    }

    /**
     * Creates a timestamped backup of the config file in the backups folder.
     */
    private void createBackup(@NotNull File configFile) {
        // Create backups directory if it doesn't exist
        File backupsDir = new File(configFile.getParentFile(), "backups");
        if (!backupsDir.exists()) {
            backupsDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String backupFileName = configFile.getName() + ".backup-" + timestamp;
        File backupFile = new File(backupsDir, backupFileName);

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.save(backupFile);
            plugin.getLogger().info("Created backup: backups/" + backupFileName);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create backup: " + e.getMessage());
        }
    }

    /**
     * Logs a summary of the migration changes.
     */
    private void logMigrationSummary(@NotNull String fileName) {
        if (addedKeys.isEmpty() && removedKeys.isEmpty()) {
            plugin.getLogger().info(fileName + " migration complete - no changes needed");
            return;
        }

        plugin.getLogger().info("=== " + fileName + " Migration Summary ===");

        if (!addedKeys.isEmpty()) {
            plugin.getLogger().info("Added " + addedKeys.size() + " new key(s):");
            for (String key : addedKeys) {
                plugin.getLogger().info("  + " + key);
            }
        }

        if (!removedKeys.isEmpty()) {
            plugin.getLogger().info("Removed " + removedKeys.size() + " obsolete key(s):");
            for (String key : removedKeys) {
                plugin.getLogger().info("  - " + key);
            }
        }

        plugin.getLogger().info("=== Migration Complete ===");
    }

    /**
     * Gets the list of keys that were added during migration.
     */
    public List<String> getAddedKeys() {
        return new ArrayList<>(addedKeys);
    }

    /**
     * Gets the list of keys that were removed during migration.
     */
    public List<String> getRemovedKeys() {
        return new ArrayList<>(removedKeys);
    }
}
