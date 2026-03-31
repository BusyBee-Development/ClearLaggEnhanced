package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
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
    }

    public void startAutoClearTask() {
        if (!configManager.getBoolean("entity-clearing.enabled", true)) {
            if (configManager.getBoolean("notifications.console-notifications", true)) {
                plugin.getLogger().info("Entity clearing is disabled in config.");
            }
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

        if (warnLead >= clearInterval) {
            warnLead = Math.max(0, clearInterval - 5);
            plugin.getLogger().warning("The largest broadcast warning (" + configManager.getIntegerList("notifications.broadcast-times") + "s) " +
                    "is >= clear interval (" + clearInterval + "s). Capping largest warning to " + warnLead + "s to prevent timer overlap.");
        }

        int initialDelayTicks = intervalTicks - (warnLead * 20);
        if (initialDelayTicks < 20) {
            initialDelayTicks = 20;
        }

        nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);

        int finalWarnLead = warnLead;
        clearTask = scheduler.runTimer(() -> {
            try {
                nextClearTime = System.currentTimeMillis() + (finalWarnLead * 1000L);
                plugin.getNotificationManager().sendClearWarnings(finalWarnLead);
            } catch (Throwable t) {
                plugin.getLogger().severe("Error in entity clearing timer task: " + t.getMessage());
                t.printStackTrace();
            }
        }, initialDelayTicks, intervalTicks);

        final boolean consoleNotify = configManager.getBoolean("notifications.console-notifications", true);
        if (consoleNotify) {
            plugin.getLogger().info("Entity clearing task started with interval: " + clearInterval + " seconds");
        }
    }

    public int clearEntities() {
        final long startNanos = System.nanoTime();
        final boolean consoleNotify = configManager.getBoolean("notifications.console-notifications", true);

        if (consoleNotify) {
            plugin.getLogger().info("Automatic entity clearing started...");
        }

        final List<String> whitelist = configManager.getStringList("entity-clearing.whitelist").stream().map(String::toUpperCase).toList();
        final List<String> itemWhitelist = configManager.getStringList("entity-clearing.item-whitelist").stream().map(String::toUpperCase).toList();
        final boolean whitelistAllMobs = configManager.getBoolean("entity-clearing.whitelist-all-mobs", false);
        final boolean protectStacked = configManager.getBoolean("entity-clearing.protect-stacked-entities", true);
        final List<String> worlds = configManager.getStringList("entity-clearing.worlds");
        final List<Chunk> allChunks = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch chunkLatch = new CountDownLatch(1);
        
        scheduler.runNextTick(task -> {
            try {
                for (World world : Bukkit.getWorlds()) {
                    if (!worlds.isEmpty() && !worlds.contains(world.getName())) {
                        continue;
                    }

                    Collections.addAll(allChunks, world.getLoadedChunks());
                }
            } finally {
                chunkLatch.countDown();
            }
        });
        
        try {
            if (!chunkLatch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out while waiting for loaded chunks list from the main thread.");
            }
        } catch (InterruptedException ignored) {
        }

        if (allChunks.isEmpty()) {
            if (consoleNotify) {
                plugin.getLogger().info("No loaded chunks found to clear entities from.");
            }
            nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);
            return 0;
        } else {
            if (consoleNotify) {
                plugin.getLogger().info("Scanning " + allChunks.size() + " chunks for entities to clear...");
            }
        }

        final CountDownLatch latch = new CountDownLatch(allChunks.size());
        final AtomicInteger cleared = new AtomicInteger(0);
        final AtomicInteger skipped = new AtomicInteger(0);

        for (Chunk chunk : allChunks) {
            final World world = chunk.getWorld();
            final int x = chunk.getX();
            final int z = chunk.getZ();
            final Location loc = new Location(world, (x << 4) + 8, 64, (z << 4) + 8);

            scheduler.runAtLocation(loc, task -> {
                try {
                    for (Entity entity : chunk.getEntities()) {
                        try {
                            if (!entity.isValid() || entity.isDead()) {
                                continue;
                            }

                            boolean isStacked = stackerManager.isStacked(entity);

                            if (plugin.getEntityProtectionUtils().isProtected(entity)) {
                                skipped.incrementAndGet();
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
            if (!latch.await(30, java.util.concurrent.TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out while waiting for chunk clearing tasks to complete. Some chunks may not have been cleared.");
            }
        } catch (InterruptedException ignored) {
        }

        final long tookMs = (System.nanoTime() - startNanos) / 1_000_000L;
        nextClearTime = System.currentTimeMillis() + (clearInterval * 1000L);

        if (consoleNotify) {
            plugin.getLogger().info("Clear complete: " + cleared.get() + " cleared, " + skipped.get() + " skipped, across " + allChunks.size() + " chunks (Took " + tookMs + "ms)");
        }

        return cleared.get();
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
            if (configManager.getBoolean("notifications.console-notifications", true)) {
                plugin.getLogger().info("Entity clearing task stopped");
            }
        }
    }
}
