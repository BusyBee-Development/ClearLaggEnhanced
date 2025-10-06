package com.clearlagenhanced.listeners;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.managers.LagPreventionManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MobLimiterListener implements Listener {

    private final ClearLaggEnhanced plugin;
    private final LagPreventionManager limiter;
    private final boolean debug;

    public MobLimiterListener(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.limiter = plugin.getLagPreventionManager();
        this.debug = plugin.getConfigManager().getBoolean("debug", false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(@NotNull CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isCountable(entity)) {
            return;
        }

        Chunk chunk = entity.getLocation().getChunk();
        if (limiter.isMobLimitReached(chunk)) {
            event.setCancelled(true);
            if (debug) {
                Map<String, String> ph = new ConcurrentHashMap<>();
                ph.put("type", entity.getType().name());
                ph.put("x", String.valueOf(chunk.getX()));
                ph.put("z", String.valueOf(chunk.getZ()));
                var comp = plugin.getMessageManager().getMessage("debug.mob-limiter.cancelled", ph);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("CLE.admin"))
                        .forEach(p -> p.sendMessage(comp));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerSpawn(@NotNull SpawnerSpawnEvent event) {
        Entity entity = event.getEntity();
        if (!isCountable(entity)) {
            return;
        }

        Chunk chunk = entity.getLocation().getChunk();
        if (limiter.isMobLimitReached(chunk)) {
            event.setCancelled(true);
            if (debug) {
                Map<String, String> ph = new ConcurrentHashMap<>();
                ph.put("type", entity.getType().name());
                ph.put("x", String.valueOf(chunk.getX()));
                ph.put("z", String.valueOf(chunk.getZ()));
                var comp = plugin.getMessageManager().getMessage("debug.mob-limiter.cancelled", ph);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("CLE.admin"))
                        .forEach(p -> p.sendMessage(comp));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawnMonitor(@NotNull CreatureSpawnEvent event) {
        final Chunk chunk = event.getEntity().getLocation().getChunk();
        final Location location = event.getEntity().getLocation();
        limiter.optimizeChunk(chunk, location);
    }

    private boolean isCountable(@NotNull Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        return entity.getType() != EntityType.PLAYER;
    }

    private void debugAdmins(String message) {
        plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("CLE.admin"))
                .forEach(player -> player.sendMessage(message));
    }
}
