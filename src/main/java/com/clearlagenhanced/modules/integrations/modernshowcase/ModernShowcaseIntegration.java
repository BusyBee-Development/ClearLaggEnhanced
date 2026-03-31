package com.clearlagenhanced.modules.integrations.modernshowcase;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import lombok.Getter;
import org.bukkit.Bukkit;

public class ModernShowcaseIntegration extends Module {
    private final ClearLaggEnhanced plugin;
    @Getter private ModernShowcaseHook hook;

    public ModernShowcaseIntegration(ClearLaggEnhanced plugin) {
        super("ModernShowcase Integration", "modernshowcase");
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ModernShowcase")) {
            setEnabled(false);
            return;
        }

        hook = new ModernShowcaseHook();
        if (hook.isEnabled()) {
            plugin.getLogger().info("ModernShowcase integration enabled");
        } else {
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        hook = null;
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }
}
