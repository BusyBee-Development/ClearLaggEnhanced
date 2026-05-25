package net.busybee.clearlaggenhanced.modules.integrations.rosestacker;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class RoseStackerIntegration extends Module {

    private static final String DEPENDENCY_NAME = "RoseStacker";
    private final ClearLaggEnhanced plugin;
    private RoseStackerHook hook;
    private RoseStackerLifecycleListener lifecycleListener;

    public RoseStackerIntegration(ClearLaggEnhanced plugin) {
        super("RoseStacker Integration", "rosestacker");
        this.plugin = plugin;
    }

    @Override
    public void onRegister() {
        registerGUI("rosestacker", "RoseStacker Integration", "ROSE_BUSH", () -> null);
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
        lifecycleListener = new RoseStackerLifecycleListener(this);
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

        hook = new RoseStackerHook();
        if (hook.isEnabled()) {
            plugin.getStackerManager().registerHook(hook);
            plugin.getLogger().info("RoseStacker integration enabled");
        } else {
            hook = null;
            plugin.getLogger().warning("RoseStacker integration could not be initialized.");
        }
    }

    private void clearHook() {
        if (hook == null) {
            return;
        }
        plugin.getStackerManager().unregisterHook(hook);
        hook = null;
        plugin.getLogger().info("RoseStacker integration disabled");
    }
}
