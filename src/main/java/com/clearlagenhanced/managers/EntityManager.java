package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityManager {

    private final ClearLaggEnhanced plugin;
    private final ConfigManager configManager;
    private final StackerManager stackerManager;
    private final PlatformScheduler scheduler;
    private WrappedTask clearTask;
    private long nextClearTime;
    private int clearInterval;

    public EntityManager(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.stackerManager = plugin.getStackerManager();
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

    public int clearEntities(boolean isManual) {
        final long startNanos = System.nanoTime();

        final List<String> whitelist = configManager.getStringList("entity-clearing.whitelist");
        final List<String> itemWhitelist = configManager.getStringList("entity-clearing.item-whitelist");
        final boolean whitelistAllMobs = configManager.getBoolean("entity-clearing.whitelist-all-mobs", false);
        final boolean protectStacked = configManager.getBoolean("entity-clearing.protect-stacked-entities", true);
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

        final int BATCH_SIZE = 50;
        final List<List<Entity>> batches = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i += BATCH_SIZE) {
            batches.add(snapshot.subList(i, Math.min(i + BATCH_SIZE, snapshot.size())));
        }

        final CountDownLatch latch = new CountDownLatch(batches.size());
        final AtomicInteger cleared = new AtomicInteger(0);

        for (List<Entity> batch : batches) {
            if (batch.isEmpty()) {
                latch.countDown();
                continue;
            }

            Entity firstEntity = batch.get(0);
            scheduler.runAtEntity(firstEntity, task -> {
                try {
                    for (Entity entity : batch) {
                        try {
                            if (!entity.isValid() || entity.isDead()) {
                                continue;
                            }

                            boolean isStacked = stackerManager.isStacked(entity);

                            if (isStacked && protectStacked) {
                                continue;
                            }

                            if (!shouldClearEntity(entity, whitelist, itemWhitelist, whitelistAllMobs, isStacked)) {
                                continue;
                            }

                            if (isStacked) {
                                stackerManager.removeStack(entity);
                            } else {
                                entity.remove();
                            }
                            cleared.incrementAndGet();
                        } catch (Throwable ex) {
                            plugin.getLogger().warning("Error while clearing " + entity.getType() + ": " + ex.getMessage());
                        }
                    }
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

    private boolean shouldClearEntity(Entity entity, List<String> whitelist, List<String> itemWhitelist, boolean whitelistAllMobs, boolean isStacked) {
        EntityType type = entity.getType();
        String typeName = type.name();

        if (type == EntityType.PLAYER) return false;
        if (entity instanceof Vehicle && !entity.getPassengers().isEmpty()) return false;
        if (entity instanceof LivingEntity && entity.isInsideVehicle()) return false;
        if (whitelistAllMobs && entity instanceof LivingEntity) return false;
        if (whitelist.contains(typeName)) return false;

        if (entity instanceof Item) {
            String materialName = ((Item) entity).getItemStack().getType().name();
            if (itemWhitelist.contains(materialName)) return false;
        }

        if (configManager.getBoolean("entity-clearing.protect-named-entities", true) && entity.getCustomName() != null) {
            if (entity instanceof Item && isStacked) {
            } else {
                return false;
            }
        }

        if (configManager.getBoolean("entity-clearing.protect-tamed-entities", true) && entity instanceof Tameable tameable) {
            if (tameable.isTamed()) return false;
        }

        return true;
    }

    public long getTimeUntilNextClear() {
        if (clearTask == null || !configManager.getBoolean("entity-clearing.enabled", true)) return -1;
        long currentTime = System.currentTimeMillis();
        return Math.max(0, (nextClearTime - currentTime) / 1000);
    }

    public String getFormattedTimeUntilNextClear() {
        long seconds = getTimeUntilNextClear();
        if (seconds == -1) return "Auto Clearing is Disabled";
        if (seconds == 0) return "0s";
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes > 0 ? String.format("%dm %ds", minutes, remainingSeconds) : String.format("%ds", seconds);
    }

    public void shutdown() {
        if (clearTask != null) {
            scheduler.cancelTask(clearTask);
            clearTask = null;
            plugin.getLogger().info("Entity clearing task stopped");
        }
    }
}
