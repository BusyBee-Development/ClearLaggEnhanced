package net.busybee.clearlagenhanced.core.gui;

import net.busybee.clearlagenhanced.inventory.InventoryGUI;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModuleGUIRegistry {
    @Getter
    private final Map<String, ModuleGUIInfo> registeredGUIs = new HashMap<>();
    
    public void registerModuleGUI(String moduleId, String displayName, String iconMaterial, Supplier<InventoryGUI> guiSupplier) {
        registeredGUIs.put(moduleId, new ModuleGUIInfo(displayName, iconMaterial, guiSupplier));
    }
    
    public void unregisterModuleGUI(String moduleId) {
        registeredGUIs.remove(moduleId);
    }
    public ModuleGUIInfo getGUIInfo(String moduleId) {
        return registeredGUIs.get(moduleId);
    }
    public void clear() {
        registeredGUIs.clear();
    }
    public record ModuleGUIInfo(String displayName, String iconMaterial, Supplier<InventoryGUI> guiSupplier) {
    }
}
