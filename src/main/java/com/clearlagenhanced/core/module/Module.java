package com.clearlagenhanced.core.module;

import com.clearlagenhanced.core.gui.ModuleGUIRegistry;
import com.clearlagenhanced.inventory.InventoryGUI;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.function.Supplier;

public abstract class Module {
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
    public void setGUIRegistry(ModuleGUIRegistry guiRegistry) {
        this.guiRegistry = guiRegistry;
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

    protected java.util.List<String> getStringList(String path) {
        return config != null ? config.getStringList(path) : java.util.Collections.emptyList();
    }

    protected java.util.List<Integer> getIntegerList(String path) {
        return config != null ? config.getIntegerList(path) : java.util.Collections.emptyList();
    }
}
