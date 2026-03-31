package com.clearlagenhanced.core.module;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.gui.ModuleGUIRegistry;
import com.clearlagenhanced.utils.ConfigMigrator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ModuleManager {
    private final ClearLaggEnhanced plugin;
    private final Map<String, Module> modules;
    private final Map<String, Module> modulesByFolderName;
    private final File moduleFolder;
    private final ModuleGUIRegistry guiRegistry;

    public ModuleManager(ClearLaggEnhanced plugin, ModuleGUIRegistry guiRegistry) {
        this.plugin = plugin;
        this.modules = new HashMap<>();
        this.modulesByFolderName = new HashMap<>();
        this.moduleFolder = new File(plugin.getDataFolder(), "module");
        this.guiRegistry = guiRegistry;
    }

    public void registerModule(Module module) {
        module.setPlugin(plugin);
        module.setGUIRegistry(guiRegistry);
        modules.put(module.getName(), module);
        modules.put(module.getName().toLowerCase(), module);
        modulesByFolderName.put(module.getFolderName().toLowerCase(), module);
        plugin.getLogger().info("Registered module: " + module.getName());
    }

    public void loadAll() {
        for (Module module : new java.util.HashSet<>(modules.values())) {
            loadModule(module);
        }
    }

    private void loadModule(Module module) {
        File modFolder = new File(moduleFolder, module.getFolderName());
        if (!modFolder.exists()) {
            modFolder.mkdirs();
        }

        FileConfiguration config = loadModuleConfig(module, "config.yml");
        FileConfiguration guiConfig = loadModuleConfig(module, "inventory_gui.yml");

        module.setConfig(config);
        module.setGuiConfig(guiConfig);

        boolean enabled = resolveEnabledState(module);
        syncEnabledState(module, enabled);
        module.setEnabled(enabled);

        if (enabled) {
            try {
                module.onEnable();
                plugin.getLogger().info("Enabled module: " + module.getName());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to enable module " + module.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().info("Module " + module.getName() + " is disabled");
        }
    }

    private FileConfiguration loadModuleConfig(Module module, String fileName) {
        File modFolder = new File(moduleFolder, module.getFolderName());
        File configFile = new File(modFolder, fileName);
        String resourcePath = "module/" + module.getFolderName() + "/" + fileName;

        // Check if resource exists in JAR before calling migrator
        if (plugin.getResource(resourcePath) == null) {
            if (configFile.exists()) {
                return YamlConfiguration.loadConfiguration(configFile);
            }
            return new YamlConfiguration();
        }

        ConfigMigrator migrator = new ConfigMigrator(plugin);
        return migrator.migrate(resourcePath, configFile);
    }

    public void enableAll() {
        for (Module module : new java.util.HashSet<>(modules.values())) {
            if (module.isEnabled()) {
                module.onEnable();
            }
        }
    }

    public void disableAll() {
        for (Module module : new java.util.HashSet<>(modules.values())) {
            if (module.isEnabled()) {
                try {
                    module.onDisable();
                } catch (Exception e) {
                    plugin.getLogger().severe("Error disabling module " + module.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public void reloadAll() {
        for (Module module : new java.util.HashSet<>(modules.values())) {
            loadModule(module);
        }
    }

    public void setModuleEnabled(Module module, boolean enabled) {
        if (module == null) {
            return;
        }

        syncEnabledState(module, enabled);

        if (module.isEnabled() == enabled) {
            return;
        }

        module.setEnabled(enabled);

        try {
            if (enabled) {
                module.onEnable();
            } else {
                module.onDisable();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to " + (enabled ? "enable" : "disable") + " module " + module.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean resolveEnabledState(Module module) {
        if (usesMainToggle(module)) {
            return plugin.getConfigManager().getBoolean(getModuleTogglePath(module));
        }

        FileConfiguration moduleConfig = module.getConfig();
        return moduleConfig != null && moduleConfig.getBoolean("enabled", true);
    }

    private void syncEnabledState(Module module, boolean enabled) {
        if (usesMainToggle(module) && plugin.getConfigManager().getBoolean(getModuleTogglePath(module)) != enabled) {
            plugin.getConfigManager().set(getModuleTogglePath(module), enabled);
            plugin.getConfigManager().save();
        }

        FileConfiguration moduleConfig = module.getConfig();
        if (moduleConfig != null && (!moduleConfig.contains("enabled") || moduleConfig.getBoolean("enabled") != enabled)) {
            moduleConfig.set("enabled", enabled);
            module.saveConfig();
        }
    }

    private String getModuleTogglePath(Module module) {
        return "modules." + module.getFolderName();
    }

    private boolean usesMainToggle(Module module) {
        return plugin.getConfigManager().contains(getModuleTogglePath(module));
    }

    public Module getModule(String identifier) {
        if (identifier == null) return null;
        
        Module module = modules.get(identifier);
        if (module != null) return module;

        module = modules.get(identifier.toLowerCase());
        if (module != null) return module;

        return modulesByFolderName.get(identifier.toLowerCase());
    }

    public Map<String, Module> getModules() {
        return modules;
    }
}
