package net.busybee.clearlaggenhanced.modules.integrations.wildstacker;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class WildStackerIntegration extends Module {

    private static final String DEPENDENCY_NAME = "WildStacker";

    private final ClearLaggEnhanced plugin;
    private WildStackerHook hook;
    private WildStackerLifecycleListener lifecycleListener;

    public WildStackerIntegration(ClearLaggEnhanced plugin) {
        super("WildStacker Integration", "wildstacker");
        this.plugin = plugin;
    }

    @Override
    public void onRegister() {
        registerGUI("wildstacker", "WildStacker Integration", "NETHER_STAR", () -> null);
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled(DEPENDENCY_NAME);
    }

    @Override
    public void onEnable() {
        registerLifecycleListener();
        refreshHookState();
    }

    void onDependencyEnabled(Plugin plugin) {
        if (plugin == null || !DEPENDENCY_NAME.equals(plugin.getName())) {
            return;
        }
        refreshHookState();
    }

    void onDependencyDisabled(Plugin plugin) {
        if (plugin == null || !DEPENDENCY_NAME.equals(plugin.getName())) {
            return;
        }
        clearHook();
    }

    @Override
    public void onDisable() {
        unregisterLifecycleListener();
        clearHook();
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }

    private void registerLifecycleListener() {
        if (lifecycleListener != null) {
            return;
        }
        lifecycleListener = new WildStackerLifecycleListener(this);
        Bukkit.getPluginManager().registerEvents(lifecycleListener, plugin);
    }

    private void unregisterLifecycleListener() {
        if (lifecycleListener == null) {
            return;
        }
        HandlerList.unregisterAll(lifecycleListener);
        lifecycleListener = null;
    }

    private void refreshHookState() {
        if (!Bukkit.getPluginManager().isPluginEnabled(DEPENDENCY_NAME)) {
            return;
        }
        if (hook != null) {
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

    private void clearHook() {
        if (hook == null) {
            return;
        }
        plugin.getStackerManager().unregisterHook(hook);
        hook = null;
        plugin.getLogger().info("WildStacker integration disabled");
    }
}
