package com.clearlagenhanced.modules.moblimiter.models;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.concurrent.atomic.AtomicInteger;

public class LagPreventionManager {

    private final ClearLaggEnhanced plugin;
    private final Module module;
    private final int maxMobsPerChunk;

    public LagPreventionManager(ClearLaggEnhanced plugin, Module module) {
        this.plugin = plugin;
        this.module = module;
        this.maxMobsPerChunk = module.getConfig().getInt("max-mobs-per-chunk", 50);
    }

    public boolean isMobLimitReached(Chunk chunk) {
        AtomicInteger count = new AtomicInteger(0);

        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity && !plugin.getEntityProtectionUtils().isProtected(entity)) {
                count.incrementAndGet();
            }
        }

        return count.get() >= maxMobsPerChunk;
    }

    public void optimizeChunk(Chunk chunk) {
    }
}
