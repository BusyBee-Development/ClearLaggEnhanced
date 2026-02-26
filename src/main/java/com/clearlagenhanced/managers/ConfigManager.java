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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static final int CURRENT_CONFIG_VERSION = 5;

    private final ClearLaggEnhanced plugin;
    @Getter private FileConfiguration config;

    public ConfigManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        checkConfigVersion();
    }

    private void checkConfigVersion() {
        int configVersion = config.getInt("config-version", 0);

        if (configVersion < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().info("Config version " + configVersion + " is outdated. Updating to version " + CURRENT_CONFIG_VERSION + "...");
            migrateConfig(configVersion);
        } else if (configVersion > CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("Config version " + configVersion + " is newer than supported version " + CURRENT_CONFIG_VERSION + "!");
            plugin.getLogger().warning("Please update the plugin or reset your config.");
        }
    }

    private void migrateConfig(int fromVersion) {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            File backupFile = new File(plugin.getDataFolder(), "config.yml.backup-v" + fromVersion);

            if (configFile.exists()) {
                YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
                oldConfig.save(backupFile);
                plugin.getLogger().info("Backed up old config to: " + backupFile.getName());
            }

            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

            InputStream defaultStream = plugin.getResource("config.yml");
            if (defaultStream == null) {
                plugin.getLogger().severe("Could not find default config.yml in plugin JAR!");
                return;
            }

            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));

            mergeConfigs(userConfig, defaultConfig);

            userConfig.set("config-version", CURRENT_CONFIG_VERSION);

            userConfig.save(configFile);
            this.config = userConfig;
            plugin.getLogger().info("Config successfully updated to version " + CURRENT_CONFIG_VERSION);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to migrate config: " + e.getMessage());
        }
    }

    /**
     * Recursively merges default values into the user config, prioritizing user's existing values.
     * New sections and keys from the default config will be added.
     * @param userConfig The user's current configuration.
     * @param defaultConfig The plugin's default configuration.
     */
    private void mergeConfigs(@NotNull FileConfiguration userConfig, @NotNull FileConfiguration defaultConfig) {
        for (String key : defaultConfig.getKeys(false)) {
            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultConfig.get(key));
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
