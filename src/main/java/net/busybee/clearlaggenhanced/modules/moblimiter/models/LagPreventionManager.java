package net.busybee.clearlaggenhanced.modules.moblimiter.models;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.managers.EntityProtectionUtils;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LagPreventionManager {

    private final ClearLaggEnhanced plugin;
    private final int maxMobsPerChunk;
    private final boolean countKeptMobs;

    public LagPreventionManager(ClearLaggEnhanced plugin, Module module) {
        this.plugin = plugin;
        this.maxMobsPerChunk = module.getConfig().getInt("max-mobs-per-chunk", 50);
        this.countKeptMobs = module.getConfig().getBoolean("count-kept-mobs", true);
    }

    public boolean isMobLimitReached(Chunk chunk) {
        return isMobLimitReached(chunk, plugin.getEntityProtectionUtils().createProtectionContext());
    }

    public boolean isMobLimitReached(Chunk chunk, EntityProtectionUtils.ProtectionContext protectionContext) {
        return countMobs(chunk, protectionContext, null).total() >= maxMobsPerChunk;
    }

    public boolean isCountable(@NotNull Entity entity, @Nullable EntityProtectionUtils.ProtectionContext protectionContext) {
        return entity instanceof LivingEntity
                && (protectionContext == null
                    || !plugin.getEntityProtectionUtils().isExemptFromMobLimits(entity, protectionContext, countKeptMobs));
    }

    /**
     * Counts the mobs in the chunk that count towards the limits, in one pass over its entities.
     *
     * @param type when not null, also counts how many of them are this type
     */
    public @NotNull ChunkCount countMobs(@NotNull Chunk chunk, @Nullable EntityProtectionUtils.ProtectionContext protectionContext,
                                         @Nullable EntityType type) {
        int total = 0;
        int ofType = 0;
        for (Entity entity : chunk.getEntities()) {
            if (isCountable(entity, protectionContext)) {
                total++;
                if (entity.getType() == type) {
                    ofType++;
                }
            }
        }
        return new ChunkCount(total, ofType);
    }

    public int getMaxMobsPerChunk() {
        return maxMobsPerChunk;
    }

    public record ChunkCount(int total, int ofType) {
    }
}
