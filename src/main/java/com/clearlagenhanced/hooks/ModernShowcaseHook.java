package com.clearlagenhanced.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class ModernShowcaseHook {

    private static final String PLUGIN_NAME = "ModernShowcase";
    private static final String METADATA_KEY = "ModernShowcase";
    private boolean enabled;

    public ModernShowcaseHook() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isShowcaseEntity(Entity entity) {
        if (!enabled) return false;
        
        // ModernShowcase marks its entities with "ModernShowcase" metadata
        return entity.hasMetadata(METADATA_KEY);
    }
}
