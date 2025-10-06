package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityManager {

    private final ClearLaggEnhanced plugin;
    private final ConfigManager configManager;
    private final PlatformScheduler scheduler;
    private WrappedTask clearTask;
    private long nextClearTime;
    private int clearInterval;

    public EntityManager(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.scheduler = ClearLaggEnhanced.scheduler();
        startAutoClearTask();
    }

    private void startAutoClearTask() {
        if (!configManager.getBoolean("entity-clearing.enabled", true)) {
            return;
        }

        clearInterval = configManager.getInt("entity-clearing.interval", 300);
        int intervalTicks = clearInterval * 20;

        nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);

        clearTask = scheduler.runTimer(() -> {
            nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);
            plugin.getNotificationManager().sendClearWarnings();
        }, intervalTicks, intervalTicks);

        plugin.getLogger().info("Entity clearing task started with interval: " + clearInterval + " seconds");
    }

    public int clearEntities() {
        scheduler.runNextTick(task -> {
            List<String> whitelist = configManager.getStringList("entity-clearing.whitelist");
            List<String> worlds = configManager.getStringList("entity-clearing.worlds");

            final AtomicInteger cleared = new AtomicInteger(0);
            final AtomicInteger scheduled = new AtomicInteger(0);

            for (World world : Bukkit.getWorlds()) {
                if (!worlds.isEmpty() && !worlds.contains(world.getName())) {
                    continue;
                }

                for (Entity entity : world.getEntities()) {
                    if (shouldClearEntity(entity, whitelist)) {
                        scheduled.incrementAndGet();
                        scheduler.runAtEntity(entity, t -> {
                            if (!entity.isDead() && shouldClearEntity(entity, whitelist)) {
                                entity.remove();
                                cleared.incrementAndGet();
                            }

                            if (scheduled.decrementAndGet() == 0) {
                                if (configManager.getBoolean("notifications.console-notifications", true)) {
                                    plugin.getLogger().info("Cleared " + cleared.get() + " entities");
                                }

                                nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);
                            }
                        });
                    }
                }
            }

            if (scheduled.get() == 0) {
                if (configManager.getBoolean("notifications.console-notifications", true)) {
                    plugin.getLogger().info("Cleared 0 entities");
                }

                nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);
            }
        });

        return 0;
    }

    private boolean shouldClearEntity(Entity entity, List<String> whitelist) {
        EntityType type = entity.getType();
        String typeName = type.name();

        if (type == EntityType.PLAYER) {
            return false;
        }

        if (whitelist.contains(typeName)) {
            return false;
        }

        if (configManager.getBoolean("entity-clearing.protect-named-entities", true) && entity.getCustomName() != null) {
            return false;
        }

        if (configManager.getBoolean("entity-clearing.protect-tamed-entities", true) && entity instanceof Tameable tameable) {
            return !tameable.isTamed();
        }

        return true;
    }

    public long getTimeUntilNextClear() {
        if (clearTask == null || !configManager.getBoolean("entity-clearing.enabled", true)) {
            return -1;
        }

        long currentTime = System.currentTimeMillis();
        long timeUntil = (nextClearTime - currentTime) / 1000;

        return Math.max(0, timeUntil);
    }

    public String getFormattedTimeUntilNextClear() {
        long seconds = getTimeUntilNextClear();

        if (seconds == -1) {
            return "Auto-clear disabled";
        }

        if (seconds == 0) {
            return "Any moment now";
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public void shutdown() {
        if (clearTask != null) {
            scheduler.cancelTask(clearTask);
            clearTask = null;
            plugin.getLogger().info("Entity clearing task stopped");
        }
    }
}
