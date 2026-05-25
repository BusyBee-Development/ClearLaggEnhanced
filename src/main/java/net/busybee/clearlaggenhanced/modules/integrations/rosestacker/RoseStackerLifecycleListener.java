package net.busybee.clearlaggenhanced.modules.integrations.rosestacker;
    
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class RoseStackerLifecycleListener implements Listener {

    private final RoseStackerIntegration integration;

    public RoseStackerLifecycleListener(RoseStackerIntegration integration) {
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
