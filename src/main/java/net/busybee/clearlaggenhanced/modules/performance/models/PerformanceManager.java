package net.busybee.clearlaggenhanced.modules.performance.models;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.utils.MessageUtils;
import net.busybee.clearlaggenhanced.core.scheduler.PluginScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.busybee.clearlaggenhanced.core.Module;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceManager {

    private static final long SNAPSHOT_COOLDOWN_MS = 300_000L;

    private final ClearLaggEnhanced plugin;
    private final PluginScheduler scheduler;
    private GlobalEntityRegistry entityRegistry;

    public PerformanceManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.scheduler = ClearLaggEnhanced.scheduler();
        this.entityRegistry = new GlobalEntityRegistry(plugin);
    }

    // Paper and Folia both have getTPS (Folia answers for the calling region); the plugin needs Paper
    // anyway, so the old PaperConfig class check, which Paper 1.19 removed, only ever hid the real value.
    public double getTPS() {
        try {
            return Math.min(20.0, Bukkit.getServer().getTPS()[0]);
        } catch (Throwable e) {
            return 20.0;
        }
    }

    public long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public double getMemoryUsagePercentage() {
        return (double) getUsedMemory() / getMaxMemory() * 100.0;
    }

    public String getFormattedMemoryUsage() {
        long used = getUsedMemory() / 1024 / 1024;
        long max = getMaxMemory() / 1024 / 1024;
        return used + "MB / " + max + "MB";
    }

    private int cachedTotalEntities = 0;
    private ScheduledTask updateTask;

    public int getTotalEntities() {
        return cachedTotalEntities;
    }

    public void updateEntityCount() {
        this.cachedTotalEntities = entityRegistry.getGlobalEntityCount();

        if (getTPS() < 17.0) {
            takeSnapshot();
        }
    }

    public void start() {
        entityRegistry.init();
        if (updateTask != null) return;
        updateTask = scheduler.runTimer(this::updateEntityCount, 1L, 100L);
    }

    private long lastSnapshot = 0;

    public void takeSnapshot() {
        long now = System.currentTimeMillis();
        if (now - lastSnapshot < SNAPSHOT_COOLDOWN_MS) return;
        lastSnapshot = now;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tps", String.format("%.2f", getTPS()));
        MessageUtils.broadcastPermissionMessage("performance.snapshot-triggered", placeholders, "CLE.performance.snapshot", true);

        File snapshotsDir = new File(plugin.getDataFolder(), "snapshots");
        if (!snapshotsDir.exists()) snapshotsDir.mkdirs();

        String filename = "snapshot-" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt";
        File file = new File(snapshotsDir, filename);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("ClearLaggEnhanced Lag Snapshot");
            writer.println("==============================");
            writer.println("Time: " + new Date());
            writer.println("TPS: " + String.format("%.2f", getTPS()));
            writer.println("Memory: " + getFormattedMemoryUsage() + " (" + String.format("%.2f", getMemoryUsagePercentage()) + "%)");
            writer.println("Total Entities: " + getTotalEntities());
            writer.println("Online Players: " + Bukkit.getOnlinePlayers().size());
            writer.println();
            writer.println("Top 10 Laggy Chunks:");
            writer.println("-------------------");

            List<ChunkInfo> chunks = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    chunks.add(new ChunkInfo(chunk.getX(), chunk.getZ(), chunk.getEntities().length, 0));
                }
            }
            chunks.sort((a, b) -> Integer.compare(b.entityCount, a.entityCount));

            int limit = Math.min(chunks.size(), 10);
            for (int i = 0; i < limit; i++) {
                ChunkInfo info = chunks.get(i);
                writer.println("Chunk [" + info.x + ", " + info.z + "]: " + info.entityCount + " entities");
            }

            writer.println();
            writer.println("Entity Breakdown (Approximate):");
            writer.println("------------------------------");
            Map<EntityType, Integer> typeCounts = new HashMap<>();
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    typeCounts.put(entity.getType(), typeCounts.getOrDefault(entity.getType(), 0) + 1);
                }
            }

            typeCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(entry -> writer.println(entry.getKey().name() + ": " + entry.getValue()));

            Map<String, String> successPh = new HashMap<>();
            successPh.put("filename", filename);
            MessageUtils.broadcastPermissionMessage("performance.snapshot-saved", successPh, "CLE.performance.snapshot", true);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save lag snapshot: " + e.getMessage());
        }
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        entityRegistry.shutdown();
    }

    public void findLaggyChunksAsync(@NotNull Player player) {
        MessageUtils.sendMessage(player, "chunkfinder.scanning");

        World world = player.getWorld();
        
        Module module = plugin.getModuleManager().getModule("chunk-finder");
        org.bukkit.configuration.file.FileConfiguration config = (module != null) ? module.getConfig() : null;

        int radius = (config != null) ? config.getInt("radius", 10) : 10;
        int entityThreshold = (config != null) ? config.getInt("entity-threshold", 50) : 50;

        Location loc = player.getLocation();
        int playerX = loc.getBlockX() >> 4;
        int playerZ = loc.getBlockZ() >> 4;

        List<ChunkInfo> laggyChunks = Collections.synchronizedList(new ArrayList<>());
        int side = radius * 2 + 1;
        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(side * side);

        // Each chunk is counted on the thread that owns it (its region on Folia), then the results
        // are sent once the last one is in.
        for (int x = playerX - radius; x <= playerX + radius; x++) {
            for (int z = playerZ - radius; z <= playerZ + radius; z++) {
                final int chunkX = x;
                final int chunkZ = z;
                Location chunkCenter = new Location(world, (chunkX << 4) + 8, 64, (chunkZ << 4) + 8);
                scheduler.runAtLocation(chunkCenter, task -> {
                    try {
                        if (world.isChunkLoaded(chunkX, chunkZ)) {
                            int count = world.getChunkAt(chunkX, chunkZ).getEntities().length;
                            if (count >= entityThreshold) {
                                int distance = Math.max(Math.abs(chunkX - playerX), Math.abs(chunkZ - playerZ));
                                laggyChunks.add(new ChunkInfo(chunkX, chunkZ, count, distance));
                            }
                        }
                    } finally {
                        if (remaining.decrementAndGet() == 0) {
                            sendChunkFinderResults(player, new ArrayList<>(laggyChunks), radius);
                        }
                    }
                });
            }
        }
    }

    private void sendChunkFinderResults(@NotNull Player player, @NotNull List<ChunkInfo> laggyChunks, int radius) {
        scheduler.runAtEntity(player, task -> {
            if (laggyChunks.isEmpty()) {
                Map<String, String> ph = new ConcurrentHashMap<>();
                ph.put("radius", String.valueOf(radius));
                MessageUtils.sendMessage(player, "chunkfinder.none-found", ph);
            } else {
                MessageUtils.sendMessage(player, "chunkfinder.header");

                laggyChunks.sort((a, b) -> Integer.compare(b.entityCount, a.entityCount));

                int maxResults = Math.min(laggyChunks.size(), 10);
                for (int i = 0; i < maxResults; i++) {
                    ChunkInfo chunkInfo = laggyChunks.get(i);

                    Map<String, String> ph = new ConcurrentHashMap<>();
                    ph.put("x", String.valueOf(chunkInfo.x));
                    ph.put("z", String.valueOf(chunkInfo.z));
                    ph.put("count", String.valueOf(chunkInfo.entityCount));
                    ph.put("distance", String.valueOf(chunkInfo.distance));
                    MessageUtils.sendMessage(player, "chunkfinder.entry", ph);
                }

                if (laggyChunks.size() > 10) {
                    Map<String, String> phMore = new ConcurrentHashMap<>();
                    phMore.put("more", String.valueOf(laggyChunks.size() - 10));
                    MessageUtils.sendMessage(player, "chunkfinder.more", phMore);
                }
            }
        });
    }

    private record ChunkInfo(int x, int z, int entityCount, int distance) {
    }
}
