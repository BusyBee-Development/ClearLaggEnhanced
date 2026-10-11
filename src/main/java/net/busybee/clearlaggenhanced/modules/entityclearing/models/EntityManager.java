package net.busybee.clearlaggenhanced.modules.entityclearing.models;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.api.ClearCause;
import net.busybee.clearlaggenhanced.api.ClearResult;
import net.busybee.clearlaggenhanced.api.event.EntityClearCompleteEvent;
import net.busybee.clearlaggenhanced.api.event.EntityClearEvent;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.managers.StackerManager;
import net.busybee.clearlaggenhanced.modules.integrations.modernshowcase.ModernShowcaseHook;
import net.busybee.clearlaggenhanced.modules.integrations.modernshowcase.ModernShowcaseIntegration;
import net.busybee.clearlaggenhanced.modules.integrations.griefprevention3d.GriefPrevention3DHook;
import net.busybee.clearlaggenhanced.modules.integrations.griefprevention3d.GriefPrevention3DIntegration;
import net.busybee.clearlaggenhanced.models.ProtectionSettings;
import net.busybee.clearlaggenhanced.modules.entityclearing.EntityClearingModule;
import net.busybee.clearlaggenhanced.core.scheduler.PluginScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EntityManager {

    private final ClearLaggEnhanced plugin;
    private final Module module;
    private final StackerManager stackerManager;
    private final PluginScheduler scheduler;
    private final AtomicBoolean isClearing = new AtomicBoolean(false);

    public EntityManager(ClearLaggEnhanced plugin, Module module) {
        this.plugin = plugin;
        this.module = module;
        this.stackerManager = plugin.getStackerManager();
        this.scheduler = ClearLaggEnhanced.scheduler();
    }

    public ClearResult clearEntities(ClearCause cause) {
        if (!isClearing.compareAndSet(false, true)) {
            return ClearResult.of(ClearResult.Status.ALREADY_RUNNING);
        }

        try {
            // Bukkit rejects an event whose async flag does not match the calling thread.
            final boolean async = !Bukkit.isPrimaryThread();
            EntityClearEvent clearEvent = new EntityClearEvent(cause, async);
            Bukkit.getPluginManager().callEvent(clearEvent);
            if (clearEvent.isCancelled()) {
                return ClearResult.of(ClearResult.Status.CANCELLED);
            }

            ClearResult result = removeEntities();
            Bukkit.getPluginManager().callEvent(new EntityClearCompleteEvent(cause, result, async));
            return result;
        } finally {
            isClearing.set(false);
        }
    }

    private ClearResult removeEntities() {
        final long startNanos = System.nanoTime();
        final boolean consoleNotify = module.getConfig().getBoolean("notifications.console-notifications", false);

        if (consoleNotify) {
            plugin.getLogger().info("Automatic entity clearing started...");
        }

        final ProtectionSettings settings;
        if (module instanceof EntityClearingModule ecModule) {
            settings = ProtectionSettings.fromConfig(ecModule.getConfig(), ecModule.getEntitiesConfig());
        } else {
            settings = ProtectionSettings.fromConfig(module.getConfig());
        }

        ModernShowcaseHook msHookTemp = null;
        if (settings.modernShowcase()) {
            Module msModule = plugin.getModuleManager().getModule("modernshowcase");
            if (msModule != null && msModule.isEnabled()) {
                msHookTemp = ((ModernShowcaseIntegration) msModule).getHook();
            }
        }
        final ModernShowcaseHook msHook = msHookTemp;

        GriefPrevention3DHook gp3dHookTemp = null;
        if (settings.griefPrevention3D()) {
            Module gpModule = plugin.getModuleManager().getModule("griefprevention3d");
            if (gpModule != null && gpModule.isEnabled()) {
                gp3dHookTemp = ((GriefPrevention3DIntegration) gpModule).getHook();
            }
        }
        final GriefPrevention3DHook gp3dHook = gp3dHookTemp;

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
            if (!chunkLatch.await(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out while waiting for loaded chunks list from the main thread.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (allChunks.isEmpty()) {
            if (consoleNotify) {
                plugin.getLogger().info("No loaded chunks found to clear entities from.");
            }
            return new ClearResult(ClearResult.Status.COMPLETED, 0, 0, 0, (System.nanoTime() - startNanos) / 1_000_000L);
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

                            if (plugin.getEntityProtectionUtils().isProtected(entity, settings, msHook, gp3dHook, true)) {
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
            if (!latch.await(30, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out while waiting for chunk clearing tasks to complete. Some chunks may not have been cleared.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        final long tookMs = (System.nanoTime() - startNanos) / 1_000_000L;

        if (consoleNotify) {
            plugin.getLogger().info("Clear complete: " + cleared.get() + " cleared, " + skipped.get() + " skipped, across " + allChunks.size() + " chunks (Took " + tookMs + "ms)");
        }

        return new ClearResult(ClearResult.Status.COMPLETED, cleared.get(), skipped.get(), allChunks.size(), tookMs);
    }

    public void shutdown() {
    }
}
