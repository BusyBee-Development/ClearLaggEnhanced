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
    public void onEnable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
            setEnabled(false);
            return;
        }

        hook = new WildStackerHook();
        if (hook.isEnabled()) {
            plugin.getStackerManager().registerHook(hook);
            plugin.getLogger().info("WildStacker integration enabled");
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
