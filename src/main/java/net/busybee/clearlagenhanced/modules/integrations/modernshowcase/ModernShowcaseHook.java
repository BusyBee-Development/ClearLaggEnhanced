package net.busybee.clearlagenhanced.modules.integrations.modernshowcase;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

@Getter
public class ModernShowcaseHook {

    private static final String PLUGIN_NAME = "ModernShowcase";
    private static final String METADATA_KEY = "ModernShowcase";
    private final boolean enabled;

    public ModernShowcaseHook() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }

    public boolean isShowcaseEntity(Entity entity) {
        if (!enabled) return false;

        return entity.hasMetadata(METADATA_KEY);
    }
}
