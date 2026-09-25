package net.busybee.clearlaggenhanced.core;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.gui.ModuleGUIRegistry;
import net.busybee.clearlaggenhanced.gui.base.InventoryGUI;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public abstract class Module {
    protected ClearLaggEnhanced plugin;
    private final String name;
    private final String folderName;
    private FileConfiguration config;
    private FileConfiguration guiConfig;
    private boolean enabled;
    private ModuleGUIRegistry guiRegistry;

    public Module(String name, String folderName) {
        this.name = name;
        this.folderName = folderName;
    }

    public abstract void onEnable();
    public abstract void onDisable();
    public abstract void onReload();
    public void onRegister() {
    }
    public void setGUIRegistry(ModuleGUIRegistry guiRegistry) {
        this.guiRegistry = guiRegistry;
    }

    public void setPlugin(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
    }

    public void saveConfig() {
        if (config == null || plugin == null) return;
        try {
            File modFolder = new File(new File(plugin.getDataFolder(), "module"), folderName);
            File configFile = new File(modFolder, "config.yml");
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config for module " + name + ": " + e.getMessage());
        }
    }

    protected void registerGUI(String moduleId, String displayName, String iconMaterial, Supplier<InventoryGUI> guiSupplier) {
        if (guiRegistry != null) {
            guiRegistry.registerModuleGUI(moduleId, displayName, iconMaterial, guiSupplier);
        }
    }

    protected void unregisterGUI(String moduleId) {
        if (guiRegistry != null) {
            guiRegistry.unregisterModuleGUI(moduleId);
        }
    }

    public String getName() {
        return name;
    }
    public String getFolderName() {
        return folderName;
    }
    public FileConfiguration getConfig() {
        return config;
    }
    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }
    public void setConfig(FileConfiguration config) {
        this.config = config;
    }
    public void setGuiConfig(FileConfiguration guiConfig) {
        this.guiConfig = guiConfig;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAvailable() {
        return true;
    }

    protected boolean getBoolean(String path, boolean def) {
        return config != null ? config.getBoolean(path, def) : def;
    }

    protected int getInt(String path, int def) {
        return config != null ? config.getInt(path, def) : def;
    }
    protected double getDouble(String path, double def) {
        return config != null ? config.getDouble(path, def) : def;
    }
    protected String getString(String path, String def) {
        return config != null ? config.getString(path, def) : def;
    }

    protected List<String> getStringList(String path) {
        return config != null ? config.getStringList(path) : Collections.emptyList();
    }

    protected List<Integer> getIntegerList(String path) {
        return config != null ? config.getIntegerList(path) : Collections.emptyList();
    }
}
