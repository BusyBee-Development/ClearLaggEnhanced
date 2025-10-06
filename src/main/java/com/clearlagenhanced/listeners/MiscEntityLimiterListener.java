package com.clearlagenhanced.listeners;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.managers.ConfigManager;
import com.clearlagenhanced.managers.MiscEntitySweepService;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MiscEntityLimiterListener implements Listener {

    private final PlatformScheduler scheduler;
    private final boolean enabled;
    private final Map<EntityType, Integer> caps;
    private final Set<String> worldFilter;
    private final boolean protectNamed;
    private final Set<String> protectedTags;
    private final MiscEntitySweepService notifier;

    public MiscEntityLimiterListener(ClearLaggEnhanced plugin, MiscEntitySweepService notifier) {
        this.scheduler = ClearLaggEnhanced.scheduler();
        ConfigManager cfg = plugin.getConfigManager();
        this.notifier = notifier;

        enabled = cfg.getBoolean("lag-prevention.misc-entity-limiter.enabled", true);
        protectNamed = cfg.getBoolean("lag-prevention.misc-entity-limiter.protect.named", true);
        protectedTags = new HashSet<>(cfg.getStringList("lag-prevention.misc-entity-limiter.protect.tags"));
        worldFilter = new HashSet<>(cfg.getStringList("lag-prevention.misc-entity-limiter.worlds"));

        caps = new EnumMap<>(EntityType.class);
        loadCaps(cfg.getConfig().getConfigurationSection("lag-prevention.misc-entity-limiter.limits-per-chunk"));
    }

    private void loadCaps(@Nullable ConfigurationSection sec) {
        if (sec == null) {
            return;
        }

        for (String key : sec.getKeys(false)) {
            try {
                EntityType type = EntityType.valueOf(key.toUpperCase(Locale.ROOT));
                int cap = sec.getInt(key, -1);
                if (cap >= 0) caps.put(type, cap);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private boolean isWorldAllowed(@NotNull World world) {
        return worldFilter.isEmpty() || worldFilter.contains(world.getName());
    }

    private boolean exempt(@NotNull Entity entity) {
        if (protectNamed && entity.getCustomName() != null && !entity.getCustomName().isEmpty()) return true;
        if (!protectedTags.isEmpty()) {
            for (String tags : protectedTags) {
                if (entity.getScoreboardTags().contains(tags)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean overCapIfAdded(@NotNull Chunk chunk, @NotNull EntityType type) {
        Integer cap = caps.get(type);
        if (cap == null || cap < 0) {
            return false;
        }

        int count = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == type) {
                count++;
            }
        }

        return count >= cap;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntitySpawn(@NotNull EntitySpawnEvent event) {
        if (!enabled) {
            return;
        }

        Entity entity = event.getEntity();
        if (!caps.containsKey(entity.getType())) {
            return;
        }

        if (!isWorldAllowed(entity.getWorld())) {
            return;
        }

        if (exempt(entity)) {
            return;
        }

        if (overCapIfAdded(entity.getLocation().getChunk(), entity.getType())) {
            event.setCancelled(true);
            if (notifier != null) {
                notifier.notifyAdmins(event.getLocation().getChunk(), entity.getType(), 0, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(@NotNull HangingPlaceEvent event) {
        if (!enabled) {
            return;
        }

        Hanging hanging = event.getEntity();
        if (!caps.containsKey(hanging.getType())) {
            return;
        }

        if (!isWorldAllowed(hanging.getWorld())) {
            return;
        }

        if (overCapIfAdded(hanging.getLocation().getChunk(), hanging.getType())) {
            event.setCancelled(true);
            if (notifier != null) {
                notifier.notifyAdmins(hanging.getLocation().getChunk(), hanging.getType(), 0, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleCreate(@NotNull VehicleCreateEvent event) {
        if (!enabled) {
            return;
        }

        Entity entity = event.getVehicle();
        if (!caps.containsKey(entity.getType())) {
            return;
        }

        if (!isWorldAllowed(entity.getWorld())) {
            return;
        }

        if (overCapIfAdded(entity.getLocation().getChunk(), entity.getType())) {
            scheduler.runAtEntity(entity, task -> entity.remove());
            if (notifier != null) {
                notifier.notifyAdmins(entity.getLocation().getChunk(), entity.getType(), 1, false);
            }
        }
    }
}
