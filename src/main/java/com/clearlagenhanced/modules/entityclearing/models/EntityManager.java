package com.clearlagenhanced.modules.entityclearing.models;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.managers.StackerManager;
import com.clearlagenhanced.modules.integrations.modernshowcase.ModernShowcaseHook;
import com.clearlagenhanced.modules.integrations.modernshowcase.ModernShowcaseIntegration;
import com.clearlagenhanced.utils.ProtectionSettings;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityManager {

    private final ClearLaggEnhanced plugin;
    private final Module module;
    private final StackerManager stackerManager;
    private final PlatformScheduler scheduler;
    private final AtomicBoolean isClearing = new AtomicBoolean(false);

    public EntityManager(ClearLaggEnhanced plugin, Module module) {
        this.plugin = plugin;
        this.module = module;
        this.stackerManager = plugin.getStackerManager();
        this.scheduler = ClearLaggEnhanced.scheduler();
    }

    public int clearEntities() {
        if (!isClearing.compareAndSet(false, true)) {
            return -1;
        }

        try {
            final long startNanos = System.nanoTime();
            final boolean consoleNotify = module.getConfig().getBoolean("notifications.console-notifications", false);

            if (consoleNotify) {
                plugin.getLogger().info("Automatic entity clearing started...");
            }

            final ProtectionSettings settings = ProtectionSettings.fromConfig(module.getConfig());

            ModernShowcaseHook msHookTemp = null;
            if (settings.modernShowcase()) {
                Module msModule = plugin.getModuleManager().getModule("modernshowcase");
                if (msModule != null && msModule.isEnabled()) {
                    msHookTemp = ((ModernShowcaseIntegration) msModule).getHook();
                }
            }
            final ModernShowcaseHook msHook = msHookTemp;

            final List<String> worlds = module.getConfig().getStringList("worlds");
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
                if (!chunkLatch.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("Timed out while waiting for loaded chunks list from the main thread.");
                }
            } catch (InterruptedException ignored) {
            }

            if (allChunks.isEmpty()) {
                if (consoleNotify) {
                    plugin.getLogger().info("No loaded chunks found to clear entities from.");
                }
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

                                if (plugin.getEntityProtectionUtils().isProtected(entity, settings, msHook)) {
                                    skipped.incrementAndGet();
                                    continue;
                                }

                                if (stackerManager.isStacked(entity)) {
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

            if (consoleNotify) {
                plugin.getLogger().info("Clear complete: " + cleared.get() + " cleared, " + skipped.get() + " skipped, across " + allChunks.size() + " chunks (Took " + tookMs + "ms)");
            }

            return cleared.get();
        } finally {
            isClearing.set(false);
        }
    }

    public void shutdown() {
    }
}
