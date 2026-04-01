package com.clearlagenhanced.modules.spawnerlimiter;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.modules.spawnerlimiter.inventory.SpawnerLimiterGUI;
import com.clearlagenhanced.modules.spawnerlimiter.listeners.SpawnerLimiterListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

public class SpawnerLimiterModule extends Module {
    private final ClearLaggEnhanced plugin;
    private SpawnerLimiterListener spawnerLimiterListener;

    public SpawnerLimiterModule(ClearLaggEnhanced plugin) {
        super("Spawner Limiter", "spawner-limiter");
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        spawnerLimiterListener = new SpawnerLimiterListener(plugin, this);
        Bukkit.getPluginManager().registerEvents(spawnerLimiterListener, plugin);
        
        registerGUI("spawner-limiter", "Spawner Limiter", "SPAWNER", () -> new SpawnerLimiterGUI(plugin, this));
    }

    @Override
    public void onDisable() {
        if (spawnerLimiterListener != null) {
            HandlerList.unregisterAll(spawnerLimiterListener);
        }
        
        unregisterGUI("spawner-limiter");
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }
}
