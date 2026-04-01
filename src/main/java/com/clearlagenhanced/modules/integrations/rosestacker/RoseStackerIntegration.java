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
            plugin.getLogger().info("RoseStacker integration is enabled in config, but RoseStacker is not installed.");
            return;
        }

        hook = new RoseStackerHook();
        if (hook.isEnabled()) {
            plugin.getStackerManager().registerHook(hook);
            plugin.getLogger().info("RoseStacker integration enabled");
        } else {
            hook = null;
            plugin.getLogger().warning("RoseStacker integration could not be initialized.");
        }
    }

    @Override
    public void onDisable() {
        if (hook != null) {
            plugin.getStackerManager().unregisterHook(hook);
            hook = null;
        }
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }
}
