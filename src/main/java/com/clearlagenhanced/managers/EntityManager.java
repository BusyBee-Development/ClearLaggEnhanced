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

        final CountDownLatch latch = new CountDownLatch(snapshot.size());
        final AtomicInteger cleared = new AtomicInteger(0);

        for (Entity entity : snapshot) {
            scheduler.runAtEntity(entity, task -> {
                try {
                    if (!entity.isValid() || entity.isDead()) {
                        return;
                    }

                    boolean isStacked = stackerManager.isStacked(entity);

                    // Debug logging
                    if (configManager.getBoolean("debug.entity-clearing", false)) {
                        plugin.getLogger().info("Checking entity: " + entity.getType() +
                            " | Stacked: " + isStacked +
                            " | ProtectStacked: " + protectStacked);
                    }

                    // Protection logic: if protect-stacked is enabled, skip ONLY stacked entities
                    if (isStacked && protectStacked) {
                        if (configManager.getBoolean("debug.entity-clearing", false)) {
                            plugin.getLogger().info("Skipping stacked entity: " + entity.getType());
                        }
                        return;
                    }

                    // Check other clearing rules (whitelist, named, tamed, etc.)
                    if (!shouldClearEntity(entity, whitelist, itemWhitelist, whitelistAllMobs)) {
                        if (configManager.getBoolean("debug.entity-clearing", false)) {
                            plugin.getLogger().info("Entity protection rule triggered for: " + entity.getType());
                        }
                        return;
                    }

                    // Clear the entity (stacked or not)
                    if (isStacked) {
                        if (configManager.getBoolean("debug.entity-clearing", false)) {
                            plugin.getLogger().info("Removing stacked entity: " + entity.getType());
                        }
                        stackerManager.removeStack(entity);
                    } else {
                        if (configManager.getBoolean("debug.entity-clearing", false)) {
                            plugin.getLogger().info("Removing non-stacked entity: " + entity.getType());
                        }
                        entity.remove();
                    }
                    cleared.incrementAndGet();
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

    private boolean shouldClearEntity(Entity entity, List<String> whitelist, List<String> itemWhitelist, boolean whitelistAllMobs) {
        EntityType type = entity.getType();
        String typeName = type.name();
        boolean debug = configManager.getBoolean("debug.entity-clearing", false);

        if (type == EntityType.PLAYER) {
            if (debug) plugin.getLogger().info("  -> Protection: Player entity");
            return false;
        }

        // Don't clear vehicles (boats, minecarts) if they have passengers (players or mobs)
        if (entity instanceof Vehicle && !entity.getPassengers().isEmpty()) {
            if (debug) plugin.getLogger().info("  -> Protection: Vehicle with passengers");
            return false;
        }

        // Don't clear mobs that are inside vehicles
        if (entity instanceof LivingEntity && entity.isInsideVehicle()) {
            if (debug) plugin.getLogger().info("  -> Protection: Mob inside vehicle");
            return false;
        }

        // If whitelist-all-mobs is enabled, don't clear any living entities (mobs)
        if (whitelistAllMobs && entity instanceof LivingEntity) {
            if (debug) plugin.getLogger().info("  -> Protection: Whitelist-all-mobs enabled");
            return false;
        }

        if (whitelist.contains(typeName)) {
            if (debug) plugin.getLogger().info("  -> Protection: Entity type '" + typeName + "' is in whitelist");
            return false;
        }

        if (entity instanceof Item) {
            String materialName = ((Item) entity).getItemStack().getType().name();
            if (itemWhitelist.contains(materialName)) {
                if (debug) plugin.getLogger().info("  -> Protection: Item material '" + materialName + "' is in item-whitelist");
                return false;
            }
        }

        if (configManager.getBoolean("entity-clearing.protect-named-entities", true) && entity.getCustomName() != null) {
            if (debug) plugin.getLogger().info("  -> Protection: Entity has custom name");
            return false;
        }

        if (configManager.getBoolean("entity-clearing.protect-tamed-entities", true) && entity instanceof Tameable tameable) {
            if (tameable.isTamed()) {
                if (debug) plugin.getLogger().info("  -> Protection: Entity is tamed");
                return false;
            }
        }

        if (debug) plugin.getLogger().info("  -> SHOULD CLEAR: " + typeName);
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
