package com.clearlagenhanced.listeners;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.managers.ConfigManager;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RedstoneLimiterListener implements Listener {

    private static final int COST_REDSTONE_CHANGE = 1;
    private static final int COST_PISTON = 4;
    private static final int COST_DISPENSE = 2;

    private final ClearLaggEnhanced plugin;
    private final PlatformScheduler scheduler;
    private final Map<ChunkKey, Integer> creditsUsed = new ConcurrentHashMap<>();
    private final Set<String> worldFilter = new HashSet<>();
    private WrappedTask resetTask;
    private final boolean enabled;
    private final int maxPerChunk;
    private final boolean debug;

    public RedstoneLimiterListener(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.scheduler = ClearLaggEnhanced.scheduler();
        ConfigManager config = plugin.getConfigManager();
        this.enabled = config.getBoolean("lag-prevention.redstone-limiter.enabled", true);
        this.maxPerChunk = config.getInt("lag-prevention.redstone-limiter.max-redstone-per-chunk", 100);
        this.debug = config.getBoolean("debug", false);

        worldFilter.addAll(config.getStringList("lag-prevention.redstone-limiter.worlds"));

        this.resetTask = scheduler.runTimer(creditsUsed::clear, 1L, 1L);
    }

    public void shutdown() {
        if (resetTask != null) {
            scheduler.cancelTask(resetTask);
            resetTask = null;
        }
    }

    private boolean enabled() {
        return enabled;
    }

    private int maxPerChunk() {
        return maxPerChunk;
    }

    private boolean isWorldAllowed(World world) {
        return worldFilter.isEmpty() || worldFilter.contains(world.getName());
    }

    private ChunkKey key(Chunk chunk) {
        return new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    private boolean budgetExceeded(@NotNull Chunk chunk, int cost) {
        if (!enabled()) {
            return false;
        }

        if (!isWorldAllowed(chunk.getWorld())) {
            return false;
        }

        ChunkKey key = key(chunk);
        int newUsed = creditsUsed.merge(key, cost, Integer::sum);
        return newUsed > maxPerChunk();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRedstoneChange(@NotNull BlockRedstoneEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        if (event.getNewCurrent() == event.getOldCurrent()) return;

        boolean exceeded = budgetExceeded(chunk, COST_REDSTONE_CHANGE);
        if (exceeded) {
            event.setNewCurrent(event.getOldCurrent());
            if (debug) {
                Map<String, String> ph = getStringStringMap(block, chunk);
                var comp = plugin.getMessageManager().getMessage("debug.redstone.neutralized", ph);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("CLE.admin"))
                        .forEach(p -> p.sendMessage(comp));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(@NotNull BlockPistonExtendEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        boolean exceeded = budgetExceeded(chunk, COST_PISTON);
        if (exceeded) {
            event.setCancelled(true);
            if (debug) {
                Map<String, String> ph = getStringStringMap(event.getBlock(), chunk);
                var comp = plugin.getMessageManager().getMessage("debug.redstone.piston-extend", ph);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("CLE.admin"))
                        .forEach(p -> p.sendMessage(comp));
            }
        }
    }

    private static @NotNull Map<String, String> getStringStringMap(@NotNull Block event, @NotNull Chunk chunk) {
        Map<String, String> ph = new ConcurrentHashMap<>();
        ph.put("world", event.getWorld().getName());
        ph.put("x", String.valueOf(event.getLocation().getBlockX()));
        ph.put("y", String.valueOf(event.getLocation().getBlockY()));
        ph.put("z", String.valueOf(event.getLocation().getBlockZ()));
        ph.put("cx", String.valueOf(chunk.getX()));
        ph.put("cz", String.valueOf(chunk.getZ()));
        return ph;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(@NotNull BlockPistonRetractEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        boolean exceeded = budgetExceeded(chunk, COST_PISTON);
        if (exceeded) {
            event.setCancelled(true);
            if (debug) {
                Map<String, String> ph = getStringStringMap(event.getBlock(), chunk);
                var comp = plugin.getMessageManager().getMessage("debug.redstone.piston-retract", ph);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> player.hasPermission("CLE.admin"))
                        .forEach(player -> player.sendMessage(comp));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispense(@NotNull BlockDispenseEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        boolean exceeded = budgetExceeded(chunk, COST_DISPENSE);
        if (exceeded) {
            event.setCancelled(true);
            if (debug) {
                Map<String, String> ph = getStringStringMap(event.getBlock(), chunk);
                var comp = plugin.getMessageManager().getMessage("debug.redstone.dispense", ph);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("CLE.admin"))
                        .forEach(p -> p.sendMessage(comp));
            }
        }
    }

    private static String loc(@NotNull Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private void debugAdmins(@NotNull String message) {
        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("CLE.admin"))
                .forEach(p -> p.sendMessage(message));
    }

    private record ChunkKey(@NotNull String world, int x, int z) {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof ChunkKey k)) {
                return false;
            }

            return x == k.x && z == k.z && world.equals(k.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, z);
        }
    }
}
