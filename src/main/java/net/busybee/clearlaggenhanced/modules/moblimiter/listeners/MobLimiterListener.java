package net.busybee.clearlaggenhanced.modules.moblimiter.listeners;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.modules.moblimiter.models.LagPreventionManager;
import net.busybee.clearlaggenhanced.managers.EntityProtectionUtils;
import net.busybee.clearlaggenhanced.utils.ChunkUtils;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MobLimiterListener implements Listener {

    private final ClearLaggEnhanced plugin;
    private final LagPreventionManager limiter;
    private final boolean ignoreSpawners;
    private final boolean enablePerTypeLimits;
    private final Map<EntityType, Integer> perTypeLimits = new HashMap<>();

    public MobLimiterListener(@NotNull ClearLaggEnhanced plugin, @NotNull Module module) {
        this.plugin = plugin;
        this.limiter = plugin.getLagPreventionManager();

        this.ignoreSpawners = module.getConfig().getBoolean("ignore-spawners", false);
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
        if (ignoreSpawners && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        checkLimits(event.getEntity(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerSpawn(@NotNull SpawnerSpawnEvent event) {
        if (ignoreSpawners) {
            return;
        }
        checkLimits(event.getEntity(), event);
    }

    private void checkLimits(@NotNull Entity entity, @NotNull Cancellable event) {
        EntityProtectionUtils.ProtectionContext protectionContext = plugin.getEntityProtectionUtils().createProtectionContext();
        if (!limiter.isCountable(entity, protectionContext)) {
            return;
        }

        Chunk chunk = ChunkUtils.getChunkAtIfLoaded(entity.getLocation());
        if (chunk == null) return;

        EntityType entityType = entity.getType();
        Integer typeLimit = enablePerTypeLimits ? perTypeLimits.get(entityType) : null;

        // One pass over the chunk gives both the total and the count of this type.
        LagPreventionManager.ChunkCount count = limiter.countMobs(chunk, protectionContext, typeLimit != null ? entityType : null);
        if (count.total() >= limiter.getMaxMobsPerChunk() || (typeLimit != null && count.ofType() >= typeLimit)) {
            event.setCancelled(true);
        }
    }
}
