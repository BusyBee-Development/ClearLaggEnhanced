package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConfigManager {

    private static final String HASH_TRACKING_FILE = ".config-hashes.yml";
    private static final List<String> MANAGED_CONFIGS = Arrays.asList("config.yml", "messages.yml");

    private final ClearLaggEnhanced plugin;
    @Getter private FileConfiguration config;

    public ConfigManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        autoUpdateAllConfigs();

        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    /**
     * Automatically updates all managed YML configuration files on startup.
     * Detects changes via schema hashing and merges new keys while preserving user values.
     */
    private void autoUpdateAllConfigs() {
        File hashTrackingFile = new File(plugin.getDataFolder(), HASH_TRACKING_FILE);
        FileConfiguration hashTracking = YamlConfiguration.loadConfiguration(hashTrackingFile);

        for (String configFileName : MANAGED_CONFIGS) {
            try {
                InputStream defaultStream = plugin.getResource(configFileName);
                if (defaultStream == null) {
                    plugin.getLogger().warning("Could not find " + configFileName + " in plugin JAR!");
                    continue;
                }

                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                String currentHash = generateConfigSchemaHash(defaultConfig);

                File userConfigFile = new File(plugin.getDataFolder(), configFileName);

                String storedHash = hashTracking.getString(configFileName, null);

                if (storedHash == null) {
                    if (!userConfigFile.exists()) {
                        plugin.getLogger().info("Creating " + configFileName + "...");
                        plugin.saveResource(configFileName, false);
                    } else {
                        plugin.getLogger().info("Registering " + configFileName + " for auto-updates...");
                    }
                    hashTracking.set(configFileName, currentHash);
                } else if (!storedHash.equals(currentHash)) {
                    plugin.getLogger().info("Detected changes in " + configFileName + " - auto-updating...");
                    updateConfig(configFileName, defaultConfig, userConfigFile);
                    hashTracking.set(configFileName, currentHash);
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to process " + configFileName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        try {
            hashTracking.save(hashTrackingFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save hash tracking file: " + e.getMessage());
        }
    }

    private void updateConfig(@NotNull String configFileName, @NotNull FileConfiguration defaultConfig, @NotNull File userConfigFile) {
        try {
            File backupFile = new File(plugin.getDataFolder(),
                    configFileName + ".backup-" + System.currentTimeMillis());

            if (userConfigFile.exists()) {
                YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(userConfigFile);
                oldConfig.save(backupFile);
                plugin.getLogger().info("Backed up " + configFileName + " to: " + backupFile.getName());
            }

            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(userConfigFile);

            mergeConfigs(userConfig, defaultConfig);

            userConfig.save(userConfigFile);
            plugin.getLogger().info("Successfully updated " + configFileName + "!");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update " + configFileName + ": " + e.getMessage());
        }
    }

    private String generateConfigSchemaHash(@NotNull FileConfiguration config) {
        try {
            List<String> allKeys = collectAllKeys(config, "");
            Collections.sort(allKeys);

            MessageDigest md = MessageDigest.getInstance("MD5");
            for (String key : allKeys) {
                md.update(key.getBytes(StandardCharsets.UTF_8));
            }

            byte[] digest = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            plugin.getLogger().severe("Failed to generate config schema hash: " + e.getMessage());
            return "error";
        }
    }

    private List<String> collectAllKeys(@NotNull ConfigurationSection section, @NotNull String parentPath) {
        List<String> keys = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            String fullPath = parentPath.isEmpty() ? key : parentPath + "." + key;

            if (section.isConfigurationSection(key)) {
                ConfigurationSection subSection = section.getConfigurationSection(key);
                if (subSection != null) {
                    keys.addAll(collectAllKeys(subSection, fullPath));
                }
            } else {
                keys.add(fullPath);
            }
        }

        return keys;
    }

    private void mergeConfigs(@NotNull FileConfiguration userConfig, @NotNull FileConfiguration defaultConfig) {
        for (String key : defaultConfig.getKeys(false)) {
            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultConfig.get(key));
                userConfig.setComments(key, defaultConfig.getComments(key));
                userConfig.setInlineComments(key, defaultConfig.getInlineComments(key));
            } else if (defaultConfig.isConfigurationSection(key)) {
                ConfigurationSection userSection = userConfig.getConfigurationSection(key);
                ConfigurationSection defaultSection = defaultConfig.getConfigurationSection(key);
                if (userSection != null && defaultSection != null) {
                    mergeConfigs(userSection, defaultSection);
                }
            }
        }
    }

    private void mergeConfigs(@NotNull ConfigurationSection userSection, @NotNull ConfigurationSection defaultSection) {
        for (String key : defaultSection.getKeys(false)) {
            if (!userSection.contains(key)) {
                userSection.set(key, defaultSection.get(key));
                userSection.setComments(key, defaultSection.getComments(key));
                userSection.setInlineComments(key, defaultSection.getInlineComments(key));
            } else if (defaultSection.isConfigurationSection(key)) {
                ConfigurationSection userSubSection = userSection.getConfigurationSection(key);
                ConfigurationSection defaultSubSection = defaultSection.getConfigurationSection(key);
                if (userSubSection != null && defaultSubSection != null) {
                    mergeConfigs(userSubSection, defaultSubSection);
                }
            }
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
