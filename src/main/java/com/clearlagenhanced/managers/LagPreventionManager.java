package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LagPreventionManager {

    private final ClearLaggEnhanced plugin;
    private final PlatformScheduler scheduler;
    private final ConfigManager configManager;
    private final AtomicInteger maxMobsPerChunk;
    private final boolean mobLimiterEnabled;

    public LagPreventionManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.scheduler = ClearLaggEnhanced.scheduler();
        this.mobLimiterEnabled = configManager.getBoolean("lag-prevention.mob-limiter.enabled", true);
        this.maxMobsPerChunk = new AtomicInteger(configManager.getInt("lag-prevention.mob-limiter.max-mobs-per-chunk", 50));
    }

    public boolean isMobLimitReached(@NotNull Chunk chunk) {
        if (!mobLimiterEnabled) {
            return false;
        }

        AtomicInteger mobCount = new AtomicInteger(0);
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity && !plugin.getEntityProtectionUtils().isProtected(entity)) {
                mobCount.incrementAndGet();
            }
        }

        return mobCount.get() >= maxMobsPerChunk.get();
    }

    public void optimizeChunk(@NotNull Chunk chunk) {
        scheduler.runAtLocation(chunk.getBlock(0, 0, 0).getLocation(), task -> {
            if (!isMobLimitReached(chunk)) {
                return;
            }

            AtomicInteger livingCount = new AtomicInteger(0);
            for (Entity e : chunk.getEntities()) {
                if (e instanceof LivingEntity && !plugin.getEntityProtectionUtils().isProtected(e)) {
                    livingCount.incrementAndGet();
                }
            }

            AtomicInteger over = new AtomicInteger(livingCount.get() - maxMobsPerChunk.get());
            if (over.get() <= 0) {
                return;
            }

        final AtomicInteger remaining = new AtomicInteger(over.get());

            for (Entity entity : chunk.getEntities()) {
                if (remaining.get() <= 0) break;

                if (!plugin.getEntityProtectionUtils().isProtected(entity)) {

                    final Entity toRemove = entity;

                    scheduler.runAtEntity(toRemove, task1 -> {
                        int before = remaining.getAndUpdate(curr -> curr > 0 ? curr - 1 : curr);
                        if (before <= 0) return;

                        boolean removed = false;

                        if (!plugin.getEntityProtectionUtils().isProtected(toRemove) && !toRemove.isDead()) {
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
