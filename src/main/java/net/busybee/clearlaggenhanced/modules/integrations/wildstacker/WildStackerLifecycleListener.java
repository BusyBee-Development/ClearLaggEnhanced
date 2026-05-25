package net.busybee.clearlaggenhanced.modules.integrations.wildstacker;
    
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class WildStackerLifecycleListener implements Listener {

    private final WildStackerIntegration integration;

    public WildStackerLifecycleListener(WildStackerIntegration integration) {
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
