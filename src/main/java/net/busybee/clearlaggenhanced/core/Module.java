package net.busybee.clearlaggenhanced.core;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.updater.ConfigFiles;
import net.busybee.clearlaggenhanced.gui.ModuleGUIRegistry;
import net.busybee.clearlaggenhanced.gui.base.InventoryGUI;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
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
    // False when neither config.yml nor a last working copy of it could be read; the module then holds packaged defaults.
    private boolean configValid = true;
    // True when config.yml has a mistake and the module holds its last working copy instead.
    private boolean configOnWorkingCopy;
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

    // Changes one setting and writes only that key to config.yml, leaving the rest of the owner's file as it is.
    public void setConfigValue(String path, Object value) {
        if (config == null || plugin == null) return;
        config.set(path, value);

        File modFolder = new File(new File(plugin.getDataFolder(), "module"), folderName);
        ConfigFiles.setValue(plugin, new File(modFolder, "config.yml"), path, value);
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
    public boolean isConfigValid() {
        return configValid;
    }
    public void setConfigValid(boolean configValid) {
        this.configValid = configValid;
    }
    public boolean isConfigOnWorkingCopy() {
        return configOnWorkingCopy;
    }
    public void setConfigOnWorkingCopy(boolean configOnWorkingCopy) {
        this.configOnWorkingCopy = configOnWorkingCopy;
    }

    // Whether the module may run on packaged defaults when the owner's settings cannot be read.
    // Modules that remove existing entities say no: their defaults could take what the owner protects.
    public boolean canRunOnDefaults() {
        return true;
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
