package com.clearlagenhanced.listeners;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.managers.ConfigManager;
import com.clearlagenhanced.managers.LagPreventionManager;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MobLimiterListener implements Listener {

    private final ClearLaggEnhanced plugin;
    private final LagPreventionManager limiter;
    private final boolean enablePerTypeLimits;
    private final Map<EntityType, Integer> perTypeLimits = new HashMap<>();

    public MobLimiterListener(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.limiter = plugin.getLagPreventionManager();

        // Load per-type limits from config
        ConfigManager config = plugin.getConfigManager();
        this.enablePerTypeLimits = config.getBoolean("lag-prevention.mob-limiter.per-type-limits.enabled", true);

        if (enablePerTypeLimits) {
            // Load all configured mob type limits
            Map<String, Object> limits = config.getConfigSection("lag-prevention.mob-limiter.per-type-limits.limits");
            if (limits != null) {
                for (Map.Entry<String, Object> entry : limits.entrySet()) {
                    try {
                        EntityType type = EntityType.valueOf(entry.getKey().toUpperCase());
                        int limit = Integer.parseInt(entry.getValue().toString());
                        if (limit > 0) {
                            perTypeLimits.put(type, limit);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid entity type in mob limiter config: " + entry.getKey());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(@NotNull CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isCountable(entity)) {
            return;
        }

        Chunk chunk = entity.getLocation().getChunk();
        EntityType entityType = entity.getType();

        // Check global limit first
        boolean globalLimitReached = limiter.isMobLimitReached(chunk);

        // Check per-type limit
        boolean typeLimitReached = isTypeLimitReached(chunk, entityType);

        if (globalLimitReached || typeLimitReached) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerSpawn(@NotNull SpawnerSpawnEvent event) {
        Entity entity = event.getEntity();
        if (!isCountable(entity)) {
            return;
        }

        Chunk chunk = entity.getLocation().getChunk();
        EntityType entityType = entity.getType();

        // Check global limit first
        boolean globalLimitReached = limiter.isMobLimitReached(chunk);

        // Check per-type limit
        boolean typeLimitReached = isTypeLimitReached(chunk, entityType);

        if (globalLimitReached || typeLimitReached) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawnMonitor(@NotNull CreatureSpawnEvent event) {
        final Chunk chunk = event.getEntity().getLocation().getChunk();
        limiter.optimizeChunk(chunk);
    }

    private boolean isCountable(@NotNull Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        return entity.getType() != EntityType.PLAYER;
    }

    private boolean isTypeLimitReached(@NotNull Chunk chunk, @NotNull EntityType entityType) {
        if (!enablePerTypeLimits) {
            return false;
        }

        Integer limit = perTypeLimits.get(entityType);
        if (limit == null || limit <= 0) {
            return false; // No limit configured for this type
        }

        // Count entities of this specific type in the chunk atomically
        AtomicInteger count = new AtomicInteger(0);

        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == entityType && entity instanceof LivingEntity) {
                count.incrementAndGet();
            }
        }

        return count.get() >= limit;
    }
}
