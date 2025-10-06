package com.clearlagenhanced.listeners;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.managers.ConfigManager;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HopperLimiterListener implements Listener {

    private final ClearLaggEnhanced plugin;
    private final PlatformScheduler scheduler;

    private final boolean enabled;
    private final int baseCooldownTicks;
    private final int maxHoppersPerChunk;
    private final boolean debug;
    private final Map<String, Long> lastMoveTickByChunk = new ConcurrentHashMap<>();
    private final Map<String, Long> lastLogTickByChunk = new ConcurrentHashMap<>();
    private final Map<String, Integer> hopperCountByChunk = new ConcurrentHashMap<>();

    public HopperLimiterListener(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.scheduler = ClearLaggEnhanced.scheduler();
        ConfigManager configManager = plugin.getConfigManager();

        this.enabled = configManager.getBoolean("lag-prevention.hopper-limiter.enabled", true);
        this.baseCooldownTicks = Math.max(1, configManager.getInt("lag-prevention.hopper-limiter.transfer-cooldown", 8));
        this.maxHoppersPerChunk = Math.max(0, configManager.getInt("lag-prevention.hopper-limiter.max-hoppers-per-chunk", 0));
        this.debug = configManager.getBoolean("debug", false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(@NotNull InventoryMoveItemEvent event) {
        if (!enabled) return;

        Inventory initiatorInv = event.getInitiator();

        InventoryHolder holder = initiatorInv.getHolder();
        if (!(holder instanceof Hopper hopper)) {
            return;
        }

        World world = hopper.getWorld();

        long now = world.getFullTime();
        Chunk chunk = hopper.getLocation().getChunk();
        String key = chunkKey(chunk);

        int effectiveCooldown = baseCooldownTicks;
        if (maxHoppersPerChunk > 0) {
            int count = countHoppersInChunkCached(chunk);
            if (count > maxHoppersPerChunk) {
                int excess = count - maxHoppersPerChunk;
                effectiveCooldown += Math.max(0, excess * 2);
            }
        }

        long last = lastMoveTickByChunk.getOrDefault(key, now - effectiveCooldown);

        if ((now - last) < effectiveCooldown) {
            event.setCancelled(true);
            lastMoveTickByChunk.put(key, now);
            maybeLogChunkThrottle(chunk, now);
            return;
        }

        lastMoveTickByChunk.put(key, now);
    }

    private static String chunkKey(@NotNull Chunk c) {
        return c.getWorld().getName() + ":" + c.getX() + "," + c.getZ();
    }

    private int countHoppersInChunkCached(@NotNull Chunk chunk) {
        String key = chunkKey(chunk);
        Integer cached = hopperCountByChunk.get(key);
        if (cached != null) return cached;

        Location location = new Location(chunk.getWorld(), (chunk.getX() << 4), 0, (chunk.getZ() << 4));
        scheduler.runAtLocation(location, task -> hopperCountByChunk.put(key, scanHoppersInChunk(chunk)));
        return 0;
    }

    private static int scanHoppersInChunk(@NotNull Chunk chunk) {
        final AtomicInteger count = new AtomicInteger(0);

        try {
            BlockState[] tiles = chunk.getTileEntities();
            if (tiles != null && tiles.length > 0) {
                for (BlockState st : tiles) {
                    if (st instanceof Hopper) {
                        count.incrementAndGet();
                    }
                }

                return count.get();
            }
        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
        }

        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;
        int minY = chunk.getWorld().getMinHeight();
        int maxY = chunk.getWorld().getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block b = chunk.getWorld().getBlockAt(baseX + x, y, baseZ + z);
                    if (b.getType() == Material.HOPPER) {
                        BlockState st = b.getState();
                        if (st instanceof Hopper) {
                            count.incrementAndGet();
                        }
                    }
                }
            }
        }

        return count.get();
    }

    private static String chunkKey(@NotNull World w, int cx, int cz) {
        return w.getName() + ":" + cx + "," + cz;
    }

    @EventHandler
    public void onChunkLoad(@NotNull ChunkLoadEvent e) {
        if (!enabled) return;
        Chunk chunk = e.getChunk();
        final String key = chunkKey(chunk);
        Location location = new Location(chunk.getWorld(), (chunk.getX() << 4), 0, (chunk.getZ() << 4));
        scheduler.runAtLocation(location, task -> hopperCountByChunk.put(key, scanHoppersInChunk(chunk)));
    }

    @EventHandler
    public void onChunkUnload(@NotNull ChunkUnloadEvent e) {
        if (!enabled) return;
        Chunk chunk = e.getChunk();
        String key = chunkKey(chunk);
        hopperCountByChunk.remove(key);
        lastMoveTickByChunk.remove(key);
        lastLogTickByChunk.remove(key);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent e) {
        if (!enabled) return;
        if (e.getBlockPlaced().getType() != Material.HOPPER) return;
        String key = chunkKey(e.getBlockPlaced().getChunk());
        hopperCountByChunk.merge(key, 1, Integer::sum);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent e) {
        if (!enabled) return;
        if (e.getBlock().getType() != Material.HOPPER) return;
        String key = chunkKey(e.getBlock().getChunk());
        hopperCountByChunk.compute(key, (k, v) -> {
            int current = (v == null ? 0 : v);
            current = Math.max(0, current - 1);
            return current; // keep 0 to avoid repeated lazy scans
        });
    }

    public void rescanLoadedChunks() {
        for (World w : plugin.getServer().getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                final String key = chunkKey(c);
                Location location = new Location(w, (c.getX() << 4), 0, (c.getZ() << 4));
                scheduler.runAtLocation(location, task -> {
                    hopperCountByChunk.put(key, scanHoppersInChunk(c));
                    lastMoveTickByChunk.remove(key);
                    lastLogTickByChunk.remove(key);
                });
            }
        }
    }

    private void maybeLogChunkThrottle(@NotNull Chunk chunk, long nowTick) {
        if (!debug) return;
        String key = chunkKey(chunk);
        long last = lastLogTickByChunk.getOrDefault(key, 0L);
        if ((nowTick - last) >= 100L) {
            Map<String, String> ph = new ConcurrentHashMap<>();
            ph.put("x", String.valueOf(chunk.getX()));
            ph.put("z", String.valueOf(chunk.getZ()));
            var comp = plugin.getMessageManager().getMessage("debug.hopper.throttling", ph);
            plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("CLE.admin"))
                    .forEach(p -> p.sendMessage(comp));
            lastLogTickByChunk.put(key, nowTick);
        }
    }

    private void debugAdmins(@NotNull String message) {
        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("CLE.admin"))
                .forEach(p -> p.sendMessage(message));
    }
}
