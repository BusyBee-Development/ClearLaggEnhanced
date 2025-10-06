package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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
        if (clearInterval <= 0) {
            clearInterval = 300;
            plugin.getLogger().warning("entity-clearing.interval was <= 0; defaulting to 300.");
        }

        int intervalTicks = clearInterval * 20;

        int warnLead = 0;
        List<Integer> times = configManager.getIntegerList("notifications.broadcast-times");
        if (times != null && !times.isEmpty()) {
            for (int t : times) {
                if (t > warnLead) {
                    warnLead = t;
                }
            }
        }

        int initialDelayTicks = intervalTicks - (warnLead * 20);
        if (initialDelayTicks < 1) {
            initialDelayTicks = 1;
        }

        nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);

        int finalWarnLead = warnLead;
        clearTask = scheduler.runTimer(() -> {
            nextClearTime = System.currentTimeMillis() + (finalWarnLead * 1000L);
            plugin.getNotificationManager().sendClearWarnings();
        }, initialDelayTicks, intervalTicks);

        plugin.getLogger().info("Entity clearing task started with interval: " + clearInterval + " seconds");
    }

    public int clearEntities() {
        final long startNanos = System.nanoTime();

        final List<String> whitelist = configManager.getStringList("entity-clearing.whitelist");
        final List<String> worlds = configManager.getStringList("entity-clearing.worlds");

        final List<Entity> snapshot = new ArrayList<>();
        final CountDownLatch snapshotLatch = new CountDownLatch(1);
        scheduler.runNextTick(task -> {
            for (World world : Bukkit.getWorlds()) {
                if (!worlds.isEmpty() && !worlds.contains(world.getName())) {
                    continue;
                }

                snapshot.addAll(world.getEntities());
            }

            snapshotLatch.countDown();
        });
        try {
            snapshotLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (snapshot.isEmpty()) {
            nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);
            if (configManager.getBoolean("notifications.console-notifications", true)) {
                plugin.getLogger().info("Cleared 0 entities in 0ms");
            }

            return 0;
        }

        final CountDownLatch latch = new CountDownLatch(snapshot.size());
        final AtomicInteger cleared = new AtomicInteger(0);

        for (Entity entity : snapshot) {
            scheduler.runAtEntity(entity, task -> {
                try {
                    if (!entity.isValid() || entity.isDead()) {
                        return;
                    }

                    if (shouldClearEntity(entity, whitelist)) {
                        entity.remove();
                        cleared.incrementAndGet();
                    }
                } catch (Throwable ex) {
                    plugin.getLogger().warning("Error while clearing " + entity.getType() + ": " + ex.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        final long tookMs = (System.nanoTime() - startNanos) / 1_000_000L;
        nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);

        if (configManager.getBoolean("notifications.console-notifications", true)) {
            plugin.getLogger().info("Cleared " + cleared.get() + " entities in " + tookMs + "ms");
        }

        return cleared.get();
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
            return "Auto Clearing is Disabled";
        }

        if (seconds == 0) {
            return "0s";
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
