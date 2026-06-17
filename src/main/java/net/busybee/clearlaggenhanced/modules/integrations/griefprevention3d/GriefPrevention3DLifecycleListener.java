package net.busybee.clearlaggenhanced.modules.integrations.griefprevention3d;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class GriefPrevention3DLifecycleListener implements Listener {

    private final GriefPrevention3DIntegration integration;

    public GriefPrevention3DLifecycleListener(GriefPrevention3DIntegration integration) {
        this.integration = integration;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        integration.onDependencyEnabled(event.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        integration.onDependencyDisabled(event.getPlugin());
    }
}
