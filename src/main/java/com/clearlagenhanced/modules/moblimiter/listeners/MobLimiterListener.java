package com.clearlagenhanced.modules.moblimiter.listeners;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.modules.moblimiter.models.LagPreventionManager;
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

    public MobLimiterListener(@NotNull ClearLaggEnhanced plugin, @NotNull Module module) {
        this.plugin = plugin;
        this.limiter = plugin.getLagPreventionManager();

        this.enablePerTypeLimits = module.getConfig().getBoolean("per-type-limits.enabled", true);

        if (enablePerTypeLimits && module.getConfig().isConfigurationSection("per-type-limits.limits")) {
            for (String key : module.getConfig().getConfigurationSection("per-type-limits.limits").getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    int limit = module.getConfig().getInt("per-type-limits.limits." + key, -1);
                    if (limit > 0) {
                        perTypeLimits.put(type, limit);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid entity type in mob limiter config: " + key);
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

        boolean globalLimitReached = limiter.isMobLimitReached(chunk);

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

        boolean globalLimitReached = limiter.isMobLimitReached(chunk);

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
        return entity instanceof LivingEntity && !plugin.getEntityProtectionUtils().isProtected(entity);
    }

    private boolean isTypeLimitReached(@NotNull Chunk chunk, @NotNull EntityType entityType) {
        if (!enablePerTypeLimits) {
            return false;
        }

        Integer limit = perTypeLimits.get(entityType);
        if (limit == null || limit <= 0) {
            return false;
        }

        AtomicInteger count = new AtomicInteger(0);

        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == entityType && entity instanceof LivingEntity && !plugin.getEntityProtectionUtils().isProtected(entity)) {
                count.incrementAndGet();
            }
        }

        return count.get() >= limit;
    }
}
