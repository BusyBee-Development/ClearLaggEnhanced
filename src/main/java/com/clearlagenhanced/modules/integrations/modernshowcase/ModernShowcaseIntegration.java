package com.clearlagenhanced.modules.integrations.modernshowcase;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class ModernShowcaseIntegration extends Module {
    private static final String DEPENDENCY_NAME = "ModernShowcase";

    private final ClearLaggEnhanced plugin;
    @Getter private ModernShowcaseHook hook;
    private ModernShowcaseLifecycleListener lifecycleListener;

    public ModernShowcaseIntegration(ClearLaggEnhanced plugin) {
        super("ModernShowcase Integration", "modernshowcase");
        this.plugin = plugin;
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

        lifecycleListener = new ModernShowcaseLifecycleListener(this);
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

        ModernShowcaseHook modernShowcaseHook = new ModernShowcaseHook();
        if (!modernShowcaseHook.isEnabled()) {
            return;
        }

        hook = modernShowcaseHook;
        plugin.getLogger().info("ModernShowcase integration enabled");
    }

    private void clearHook() {
        if (hook == null) {
            return;
        }

        hook = null;
        plugin.getLogger().info("ModernShowcase integration disabled");
    }
}
