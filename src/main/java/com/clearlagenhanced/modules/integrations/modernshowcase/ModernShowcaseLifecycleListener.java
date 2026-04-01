package com.clearlagenhanced.modules.integrations.modernshowcase;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class ModernShowcaseLifecycleListener implements Listener {

    private final ModernShowcaseIntegration integration;

    public ModernShowcaseLifecycleListener(ModernShowcaseIntegration integration) {
        this.integration = integration;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        integration.onDependencyEnabled(event.getPlugin());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        integration.onDependencyDisabled(event.getPlugin());
    }
}
