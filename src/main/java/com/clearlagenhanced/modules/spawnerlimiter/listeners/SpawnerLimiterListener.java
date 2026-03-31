package com.clearlagenhanced.modules.spawnerlimiter.listeners;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class SpawnerLimiterListener implements Listener {

    private final ClearLaggEnhanced plugin;
    private final Set<String> worldFilter = new HashSet<>();
    private final boolean enabled;
    private final double multiplier;

    public SpawnerLimiterListener(@NotNull ClearLaggEnhanced plugin, @NotNull Module module) {
        this.plugin = plugin;
        worldFilter.addAll(module.getConfig().getStringList("worlds"));
        this.enabled = module.getConfig().getBoolean("enabled", true);
        this.multiplier = Math.max(1.0, module.getConfig().getDouble("spawn-delay-multiplier", 1.5));
    }

    private boolean enabled() {
        return enabled;
    }
    private double multiplier() {
        return multiplier;
    }

    private boolean isWorldAllowed(@NotNull World world) {
        return worldFilter.isEmpty() || worldFilter.contains(world.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerSpawn(@NotNull SpawnerSpawnEvent event) {
        if (!enabled()) {
            return;
        }

        CreatureSpawner spawner = event.getSpawner();
        if (spawner == null || !isWorldAllowed(spawner.getWorld())) {
            return;
        }

        Chunk chunk = event.getLocation().getChunk();
        if (plugin.getLagPreventionManager().isMobLimitReached(chunk)) {
            event.setCancelled(true);
            return;
        }

        try {
            int current = Math.max(1, spawner.getDelay());
            int newDelay = (int) Math.min((long) (current * multiplier()), 32767L);
            spawner.setDelay(newDelay);
        } catch (Throwable ignored) {
        }
    }
}
