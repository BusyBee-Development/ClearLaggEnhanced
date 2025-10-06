package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.atomic.AtomicInteger;

public class LagPreventionManager {

    private final PlatformScheduler scheduler;
    private final AtomicInteger maxMobsPerChunk;
    private final boolean hopperLimiterEnabled;
    private final boolean redstoneLimiterEnabled;
    private final boolean mobLimiterEnabled;

    public LagPreventionManager(@NotNull ClearLaggEnhanced plugin) {
        ConfigManager configManager = plugin.getConfigManager();
        this.scheduler = ClearLaggEnhanced.scheduler();
        this.mobLimiterEnabled = configManager.getBoolean("lag-prevention.mob-limiter.enabled", true);
        this.maxMobsPerChunk = new AtomicInteger(configManager.getInt("lag-prevention.mob-limiter.max-mobs-per-chunk", 50));
        this.hopperLimiterEnabled = configManager.getBoolean("lag-prevention.hopper-limiter.enabled", true);
        this.redstoneLimiterEnabled = configManager.getBoolean("lag-prevention.redstone-limiter.enabled", true);
    }

    public boolean isMobLimitReached(@NotNull Chunk chunk) {
        if (!mobLimiterEnabled) {
            return false;
        }

        AtomicInteger mobCount = new AtomicInteger(0);
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity && entity.getType() != EntityType.PLAYER) {
                mobCount.incrementAndGet();
            }
        }

        return mobCount.get() >= maxMobsPerChunk.get();
    }

    public boolean isRedstoneExcessive(@NotNull Chunk chunk) {
        return false;
    }

    public boolean areHoppersExcessive(@NotNull Chunk chunk) {
        return false;
    }

    public void optimizeChunk(@NotNull Chunk chunk, @NotNull Location location) {
        scheduler.runAtLocation(location, task -> {
            if (!isMobLimitReached(chunk)) {
                return;
            }

            AtomicInteger livingCount = new AtomicInteger(0);
            for (Entity e : chunk.getEntities()) {
                if (e instanceof LivingEntity && e.getType() != EntityType.PLAYER) {
                    livingCount.incrementAndGet();
                }
            }

            AtomicInteger over = new AtomicInteger(livingCount.get() - maxMobsPerChunk.get());
            if (over.get() <= 0) {
                return;
            }

            AtomicInteger remaining = new AtomicInteger(over.get());

            for (Entity entity : chunk.getEntities()) {
                if (remaining.get() <= 0) break;

                if (entity instanceof LivingEntity
                        && entity.getType() != EntityType.PLAYER
                        && entity.getCustomName() == null) {

                    final Entity toRemove = entity;

                    scheduler.runAtEntity(toRemove, task1 -> {
                        int before = remaining.getAndUpdate(curr -> curr > 0 ? curr - 1 : curr);
                        if (before <= 0) return;

                        boolean removed = false;

                        if (toRemove.getType() != EntityType.PLAYER && toRemove.getCustomName() == null && !toRemove.isDead()) {
                            toRemove.remove();
                            removed = true;
                        }

                        if (!removed) {
                            remaining.incrementAndGet();
                        }
                    });
                }
            }
        });
    }
}
