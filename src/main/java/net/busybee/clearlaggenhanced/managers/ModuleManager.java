package net.busybee.clearlaggenhanced.managers;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.gui.ModuleGUIRegistry;
import net.busybee.clearlaggenhanced.core.updater.ConfigMigrator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModuleManager {
    private final ClearLaggEnhanced plugin;
    private final ConfigManager configManager;
    private final Map<String, Module> modules;
    private final Map<String, Module> modulesByFolderName;
    private final File moduleFolder;
    private final ModuleGUIRegistry guiRegistry;
    private final Set<String> warnedLegacyEnabledKeys;

    public ModuleManager(ClearLaggEnhanced plugin, ConfigManager configManager, ModuleGUIRegistry guiRegistry) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.modules = new HashMap<>();
        this.modulesByFolderName = new HashMap<>();
        this.moduleFolder = new File(plugin.getDataFolder(), "module");
        this.guiRegistry = guiRegistry;
        this.warnedLegacyEnabledKeys = new HashSet<>();
    }

    public void registerModule(Module module) {
        module.setPlugin(plugin);
        module.setGUIRegistry(guiRegistry);
        modules.put(module.getName(), module);
        modules.put(module.getName().toLowerCase(), module);
        modulesByFolderName.put(module.getFolderName().toLowerCase(), module);
        module.onRegister();
    }

    public void loadAll() {
        int enabledCount = 0;
        int disabledCount = 0;
        for (Module module : new java.util.HashSet<>(modules.values())) {
            loadModule(module);
            if (module.isEnabled()) {
                enabledCount++;
            } else {
                disabledCount++;
            }
        }
        plugin.getLogger().info("Modules: " + enabledCount + " enabled, " + disabledCount + " disabled.");
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
        warnIfLegacyEnabledKeyPresent(module, config);

        boolean enabled = resolveEnabledState(module);
        module.setEnabled(enabled);

        if (enabled) {
            try {
                module.onEnable();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to enable module " + module.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Module is disabled
        }
    }

    private FileConfiguration loadModuleConfig(Module module, String fileName) {
        File modFolder = new File(moduleFolder, module.getFolderName());
        File configFile = new File(modFolder, fileName);
        String resourcePath = "module/" + module.getFolderName() + "/" + fileName;

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
        String togglePath = getModuleTogglePath(module);
        if (configManager.contains(togglePath)) {
            return configManager.getBoolean(togglePath);
        }

        plugin.getLogger().warning("Missing module toggle '" + togglePath + "' in config.yml. Keeping module "
                + module.getName() + " disabled until the setting is restored.");
        return false;
    }

    private void syncEnabledState(Module module, boolean enabled) {
        String togglePath = getModuleTogglePath(module);
        if (!configManager.contains(togglePath) || configManager.getBoolean(togglePath) != enabled) {
            configManager.set(togglePath, enabled);
            configManager.save();
        }
    }

    private String getModuleTogglePath(Module module) {
        return "modules." + module.getFolderName();
    }

    private void warnIfLegacyEnabledKeyPresent(Module module, FileConfiguration config) {
        if (config == null || !config.contains("enabled")) {
            return;
        }

        String folderName = module.getFolderName().toLowerCase();
        if (!warnedLegacyEnabledKeys.add(folderName)) {
            return;
        }

        plugin.getLogger().info("Ignoring legacy key module/" + module.getFolderName()
                + "/config.yml:enabled. Use config.yml " + getModuleTogglePath(module) + " instead.");
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
