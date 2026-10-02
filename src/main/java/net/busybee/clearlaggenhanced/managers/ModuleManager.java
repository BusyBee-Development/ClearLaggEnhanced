package net.busybee.clearlaggenhanced.managers;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.gui.ModuleGUIRegistry;
import net.busybee.clearlaggenhanced.core.updater.ConfigFiles;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModuleManager {
    private static final Set<String> RETIRED_CONFIG_FOLDERS = Set.of("wildstacker", "rosestacker", "modernshowcase");

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
        String nameLower = module.getName().toLowerCase();
        String folderLower = module.getFolderName().toLowerCase();
        
        modules.put(module.getName(), module);
        modules.put(nameLower, module);
        modulesByFolderName.put(folderLower, module);
        
        module.onRegister();
    }

    public void loadAll() {
        int enabledCount = 0;
        int disabledCount = 0;
        for (Module module : new HashSet<>(modules.values())) {
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
        if (shipsBundledConfig(module)) {
            if (!modFolder.exists()) {
                modFolder.mkdirs();
            }
        } else {
            removeUnusedModuleFolder(module, modFolder);
        }

        ConfigFiles.Loaded config = loadModuleConfig(module, "config.yml");
        ConfigFiles.Loaded guiConfig = loadModuleConfig(module, "inventory_gui.yml");

        module.setConfig(config.config());
        module.setConfigValid(config.valid());
        module.setConfigOnWorkingCopy(config.onWorkingCopy());
        module.setGuiConfig(guiConfig.config());
        warnIfLegacyEnabledKeyPresent(module, config.config());

        boolean enabled = resolveEnabledState(module);
        if (enabled && !module.isConfigValid()) {
            String configPath = "module/" + module.getFolderName() + "/config.yml";
            if (module.canRunOnDefaults()) {
                plugin.getLogger().severe("Module " + module.getName() + " runs on its default settings because "
                        + configPath + " has no earlier working copy to fall back on. Fix the file and run /lagg reload.");
            } else {
                // Its defaults could remove things the owner's file protects.
                plugin.getLogger().severe("Module " + module.getName() + " was NOT started because " + configPath
                        + " has no earlier working copy to fall back on. Fix the file and run /lagg reload.");
                enabled = false;
            }
        }
        module.setEnabled(enabled);

        if (enabled) {
            try {
                module.onEnable();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to enable module " + module.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean shipsBundledConfig(Module module) {
        String prefix = "module/" + module.getFolderName() + "/";
        return plugin.getResource(prefix + "config.yml") != null
                || plugin.getResource(prefix + "inventory_gui.yml") != null;
    }

    private void removeUnusedModuleFolder(Module module, File modFolder) {
        if (!modFolder.isDirectory()) {
            return;
        }

        // Older versions shipped placeholder files for these modules; nothing reads them anymore
        if (RETIRED_CONFIG_FOLDERS.contains(module.getFolderName().toLowerCase())) {
            new File(modFolder, "config.yml").delete();
            new File(modFolder, "inventory_gui.yml").delete();
        }

        String[] remaining = modFolder.list();
        if (remaining != null && remaining.length == 0 && modFolder.delete()) {
            plugin.getLogger().info("Removed unused folder module/" + module.getFolderName());
        }
    }

    private ConfigFiles.Loaded loadModuleConfig(Module module, String fileName) {
        File modFolder = new File(moduleFolder, module.getFolderName());
        File configFile = new File(modFolder, fileName);
        String resourcePath = "module/" + module.getFolderName() + "/" + fileName;

        return ConfigFiles.load(plugin, resourcePath, configFile);
    }

    public void enableAll() {
        for (Module module : new HashSet<>(modules.values())) {
            if (module.isEnabled()) {
                module.onEnable();
            }
        }
    }

    public void disableAll() {
        for (Module module : new HashSet<>(modules.values())) {
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
        for (Module module : new HashSet<>(modules.values())) {
            loadModule(module);
        }
    }

    public void setModuleEnabled(Module module, boolean enabled) {
        if (module == null) {
            return;
        }

        if (enabled && !module.isConfigValid() && !module.canRunOnDefaults()) {
            plugin.getLogger().warning("Module " + module.getName() + " cannot be enabled until module/"
                    + module.getFolderName() + "/config.yml is fixed and /lagg reload is run.");
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
            configManager.setValue(togglePath, enabled);
        }
    }

    private String getModuleTogglePath(Module module) {
        return "modules." + module.getFolderName();
    }

        private void warnIfLegacyEnabledKeyPresent(Module module, FileConfiguration config) {
        if (config == null || !module.isConfigValid() || !config.contains("enabled")) {
            return;
        }

        String folderName = module.getFolderName().toLowerCase();
        if (!warnedLegacyEnabledKeys.add(folderName)) {
            return;
        }

        plugin.getLogger().info("Migrating legacy 'enabled' key in module/" + module.getFolderName() + "/config.yml to main config.yml...");
        config.set("enabled", null);

        File modFolder = new File(moduleFolder, module.getFolderName());
        File configFile = new File(modFolder, "config.yml");
        if (!ConfigFiles.removeKey(plugin, configFile, "enabled")) {
            plugin.getLogger().warning("Failed to save cleaned config for " + module.getName());
        }
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
