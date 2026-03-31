package com.clearlagenhanced.modules.integrations.rosestacker;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import org.bukkit.Bukkit;

public class RoseStackerIntegration extends Module {
    private final ClearLaggEnhanced plugin;
    private RoseStackerHook hook;

    public RoseStackerIntegration(ClearLaggEnhanced plugin) {
        super("RoseStacker Integration", "rosestacker");
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("RoseStacker")) {
            setEnabled(false);
            return;
        }

        hook = new RoseStackerHook();
        if (hook.isEnabled()) {
            plugin.getStackerManager().registerHook(hook);
            plugin.getLogger().info("RoseStacker integration enabled");
        } else {
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        if (hook != null) {
            plugin.getStackerManager().unregisterHook(hook);
        }
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }
}