package com.clearlagenhanced.modules.integrations.wildstacker;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import org.bukkit.Bukkit;

public class WildStackerIntegration extends Module {
    private final ClearLaggEnhanced plugin;
    private WildStackerHook hook;

    public WildStackerIntegration(ClearLaggEnhanced plugin) {
        super("WildStacker Integration", "wildstacker");
        this.plugin = plugin;
    }

    @Override
    public void onRegister() {
        registerGUI("wildstacker", "WildStacker Integration", "NETHER_STAR", () -> null);
    }

    @Override
    public void onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
            plugin.getLogger().info("WildStacker integration is enabled in config, but WildStacker is not installed.");
            return;
        }

        hook = new WildStackerHook();
        if (hook.isEnabled()) {
            plugin.getStackerManager().registerHook(hook);
            plugin.getLogger().info("WildStacker integration enabled");
        } else {
            hook = null;
            plugin.getLogger().warning("WildStacker integration could not be initialized.");
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
