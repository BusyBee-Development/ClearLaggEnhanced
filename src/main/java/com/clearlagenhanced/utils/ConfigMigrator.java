package com.clearlagenhanced.utils;

import org.bukkit.configuration.ConfigurationSection;
<<<<<<< HEAD
=======
import org.bukkit.configuration.InvalidConfigurationException;
>>>>>>> dev
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

public class ConfigMigrator {

    private final Plugin plugin;
    private final List<String> addedKeys = new ArrayList<>();
    private final List<String> removedKeys = new ArrayList<>();

    public ConfigMigrator(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration migrate(@NotNull String fileName) {
<<<<<<< HEAD
        addedKeys.clear();
        removedKeys.clear();

        File configFile = new File(plugin.getDataFolder(), fileName);

        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info("Created new " + fileName + " from defaults");
            return YamlConfiguration.loadConfiguration(configFile);
        }

        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream == null) {
            plugin.getLogger().warning("Could not find default " + fileName + " in plugin JAR!");
=======
        return migrate(fileName, new File(plugin.getDataFolder(), fileName));
    }

    public FileConfiguration migrate(@NotNull String resourcePath, @NotNull File configFile) {
        addedKeys.clear();
        removedKeys.clear();

        if (!configFile.exists()) {
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in != null) {
                    if (configFile.getParentFile() != null) {
                        configFile.getParentFile().mkdirs();
                    }
                    java.nio.file.Files.copy(in, configFile.toPath());
                    plugin.getLogger().info("Created new " + configFile.getName() + " from " + resourcePath);
                } else {
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create default " + configFile.getName() + " from " + resourcePath);
            }
            return YamlConfiguration.loadConfiguration(configFile);
        }

        YamlConfiguration userConfig = new YamlConfiguration();
        try {
            userConfig.load(configFile);
        } catch (InvalidConfigurationException | IOException e) {
            plugin.getLogger().severe("Detected corruption in " + configFile.getName() + "! Backing up and regenerating...");
            createBackupRaw(configFile, true);
            
            // Delete and regenerate from default
            configFile.delete();
            return migrate(resourcePath, configFile);
        }

        InputStream defaultStream = plugin.getResource(resourcePath);
        if (defaultStream == null) {
            // No default resource in JAR, just return what we have
>>>>>>> dev
            return userConfig;
        }

        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
        );

        if (!needsMigration(userConfig, defaultConfig)) {
<<<<<<< HEAD
            plugin.getLogger().info(fileName + " is already up to date");
            return userConfig;
        }

        createBackup(configFile);
=======
            return userConfig;
        }

        createBackupRaw(configFile, false);
>>>>>>> dev
        FileConfiguration mergedConfig = mergeAndReorder(userConfig, defaultConfig);

        try {
            mergedConfig.save(configFile);
<<<<<<< HEAD
            logMigrationSummary(fileName);
            return mergedConfig;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save migrated " + fileName + ": " + e.getMessage());
=======
            logMigrationSummary(configFile.getName());
            return mergedConfig;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save migrated " + configFile.getName() + ": " + e.getMessage());
>>>>>>> dev
            return null;
        }
    }

    private FileConfiguration mergeAndReorder(@NotNull FileConfiguration userConfig, @NotNull FileConfiguration defaultConfig) {
        YamlConfiguration newConfig = new YamlConfiguration();
        transferSection(userConfig, defaultConfig, newConfig, "");
        return newConfig;
    }

    private void transferSection(ConfigurationSection userConfig, ConfigurationSection defaultConfig, ConfigurationSection newConfig, String path) {
        for (String key : defaultConfig.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            
            if (defaultConfig.isConfigurationSection(key)) {
                ConfigurationSection newSubSection = newConfig.createSection(key);

                newConfig.setComments(key, defaultConfig.getComments(key));
                newConfig.setInlineComments(key, defaultConfig.getInlineComments(key));
                
                ConfigurationSection userSubSection = userConfig != null ? userConfig.getConfigurationSection(key) : null;
                ConfigurationSection defaultSubSection = defaultConfig.getConfigurationSection(key);
                
                if (defaultSubSection != null) {
                    transferSection(userSubSection, 
                                    defaultSubSection, newSubSection, fullPath);
                }
            } else {
                Object value;
                if (userConfig != null && userConfig.contains(key) && !userConfig.isConfigurationSection(key)) {
                    value = userConfig.get(key);
                } else {
                    value = defaultConfig.get(key);
                    addedKeys.add(fullPath);
                }
                
                newConfig.set(key, value);
                newConfig.setComments(key, defaultConfig.getComments(key));
                newConfig.setInlineComments(key, defaultConfig.getInlineComments(key));
            }
        }

        if (userConfig != null) {
            for (String userKey : userConfig.getKeys(false)) {
                if (!defaultConfig.contains(userKey)) {
                    if (userConfig.isConfigurationSection(userKey)) {
                        ConfigurationSection userSubSection = userConfig.getConfigurationSection(userKey);
                        ConfigurationSection newSubSection = newConfig.createSection(userKey);
                        if (userSubSection != null) {
                            copyRecursive(userSubSection, newSubSection);
                        }
                    } else {
                        newConfig.set(userKey, userConfig.get(userKey));
                    }
                }
            }
        }
    }

    private void copyRecursive(ConfigurationSection from, ConfigurationSection to) {
        for (String key : from.getKeys(false)) {
            if (from.isConfigurationSection(key)) {
                copyRecursive(from.getConfigurationSection(key), to.createSection(key));
            } else {
                to.set(key, from.get(key));
            }
        }
    }

    private boolean needsMigration(@NotNull FileConfiguration userConfig, @NotNull FileConfiguration defaultConfig) {
        Set<String> userKeys = getAllKeys(userConfig, "");
        Set<String> defaultKeys = getAllKeys(defaultConfig, "");

        for (String key : defaultKeys) {
            if (!userKeys.contains(key)) {
                return true;
            }
        }

        return false;
    }

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

<<<<<<< HEAD
    private void createBackup(@NotNull File configFile) {
=======
    private void createBackupRaw(@NotNull File configFile, boolean isCorrupted) {
>>>>>>> dev
        File backupsDir = new File(configFile.getParentFile(), "backups");
        if (!backupsDir.exists()) {
            backupsDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
<<<<<<< HEAD
        String backupFileName = configFile.getName() + ".backup-" + timestamp;
        File backupFile = new File(backupsDir, backupFileName);

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.save(backupFile);
            plugin.getLogger().info("Created backup: backups/" + backupFileName);
=======
        String backupFileName = configFile.getName() + (isCorrupted ? ".corrupted-" : ".backup-") + timestamp;
        File backupFile = new File(backupsDir, backupFileName);

        try {
            java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath());
            plugin.getLogger().info("Created " + (isCorrupted ? "corrupted file backup" : "backup") + ": backups/" + backupFileName);
>>>>>>> dev
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create backup: " + e.getMessage());
        }
    }

<<<<<<< HEAD
=======
    private void createBackup(@NotNull File configFile) {
        createBackupRaw(configFile, false);
    }

>>>>>>> dev
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

    public List<String> getAddedKeys() {
        return new ArrayList<>(addedKeys);
    }
    public List<String> getRemovedKeys() {
        return new ArrayList<>(removedKeys);
    }
}
