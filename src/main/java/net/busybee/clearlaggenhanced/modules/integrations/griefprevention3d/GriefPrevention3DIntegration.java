package net.busybee.clearlaggenhanced.modules.integrations.griefprevention3d;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class GriefPrevention3DIntegration extends Module {

    private static final String DEPENDENCY_NAME = "GriefPrevention3D";
    private static final String DEPENDENCY_NAME_ALT = "GriefPrevention";

    private final ClearLaggEnhanced plugin;
    @Getter private GriefPrevention3DHook hook;
    private GriefPrevention3DLifecycleListener lifecycleListener;

    public GriefPrevention3DIntegration(ClearLaggEnhanced plugin) {
        super("GriefPrevention3D Integration", "griefprevention3d");
        this.plugin = plugin;
    }

    @Override
    public void onRegister() {
        registerGUI("griefprevention3d", "GriefPrevention3D Integration", "GOLDEN_SHOVEL", () -> null);
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled(DEPENDENCY_NAME) || 
               Bukkit.getPluginManager().isPluginEnabled(DEPENDENCY_NAME_ALT);
    }

    @Override
    public void onEnable() {
        registerLifecycleListener();
        refreshHookState();
    }

    void onDependencyEnabled(Plugin plugin) {
        if (plugin == null) return;
        String name = plugin.getName();
        if (!DEPENDENCY_NAME.equals(name) && !DEPENDENCY_NAME_ALT.equals(name)) {
            return;
        }
        refreshHookState();
    }

    void onDependencyDisabled(Plugin plugin) {
        if (plugin == null) return;
        String name = plugin.getName();
        if (!DEPENDENCY_NAME.equals(name) && !DEPENDENCY_NAME_ALT.equals(name)) {
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
        lifecycleListener = new GriefPrevention3DLifecycleListener(this);
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
        Plugin gpPlugin = Bukkit.getPluginManager().getPlugin(DEPENDENCY_NAME);
        if (gpPlugin == null || !gpPlugin.isEnabled()) {
            gpPlugin = Bukkit.getPluginManager().getPlugin(DEPENDENCY_NAME_ALT);
        }

        if (gpPlugin == null || !gpPlugin.isEnabled()) {
            return;
        }

        if (hook != null) {
            return;
        }

        GriefPrevention3DHook gp3dHook = new GriefPrevention3DHook(gpPlugin);
        if (!gp3dHook.isEnabled()) {
            return;
        }
        hook = gp3dHook;
        plugin.getLogger().info("GriefPrevention3D integration enabled (hooked into " + gpPlugin.getName() + ")");
    }

    private void clearHook() {
        if (hook == null) {
            return;
        }
        hook = null;
        plugin.getLogger().info("GriefPrevention3D integration disabled");
    }
}
