package net.busybee.clearlaggenhanced.modules.performance.models;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.utils.MessageUtils;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.busybee.clearlaggenhanced.core.Module;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceManager {

    private final ClearLaggEnhanced plugin;
    private final PlatformScheduler scheduler;
    private final boolean isPaperServer;
    private GlobalEntityRegistry entityRegistry;

    public PerformanceManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.scheduler = ClearLaggEnhanced.scheduler();
        this.isPaperServer = checkPaperServer();
        this.entityRegistry = new GlobalEntityRegistry(plugin);
    }

    private boolean checkPaperServer() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public double getTPS() {
        if (isPaperServer) {
            try {
                return Bukkit.getServer().getTPS()[0];
            } catch (Exception e) {
                return 20.0;
            }
        } else {
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
    private WrappedTask updateTask;

    public int getTotalEntities() {
        return cachedTotalEntities;
    }

    public void updateEntityCount() {
        this.cachedTotalEntities = entityRegistry.getGlobalEntityCount();
        
        // Trigger lag snapshot if TPS is low
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
        // Limit snapshots to once every 5 minutes
        if (now - lastSnapshot < 300_000) return;
        lastSnapshot = now;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tps", String.format("%.2f", getTPS()));
        MessageUtils.broadcastMessage("performance.snapshot-triggered", placeholders, true, true);

        java.io.File snapshotsDir = new java.io.File(plugin.getDataFolder(), "snapshots");
        if (!snapshotsDir.exists()) snapshotsDir.mkdirs();

        String filename = "snapshot-" + new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date()) + ".txt";
        java.io.File file = new java.io.File(snapshotsDir, filename);

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file))) {
            writer.println("ClearLaggEnhanced Lag Snapshot");
            writer.println("==============================");
            writer.println("Time: " + new java.util.Date());
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
            Map<org.bukkit.entity.EntityType, Integer> typeCounts = new java.util.HashMap<>();
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
            MessageUtils.broadcastMessage("performance.snapshot-saved", successPh, true, true);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Failed to save lag snapshot: " + e.getMessage());
        }
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void findLaggyChunksAsync(@NotNull Player player) {
        MessageUtils.sendMessage(player, "chunkfinder.scanning");

        World world = player.getWorld();
        
        Module module = plugin.getModuleManager().getModule("chunk-finder");
        org.bukkit.configuration.file.FileConfiguration config = (module != null) ? module.getConfig() : null;

        int radius = (config != null) ? config.getInt("radius", 10) : 10;
        int entityThreshold = (config != null) ? config.getInt("entity-threshold", 50) : 50;

        Chunk playerChunk = player.getLocation().getChunk();
        int playerX = playerChunk.getX();
        int playerZ = playerChunk.getZ();

        List<ChunkInfo> laggyChunks = new ArrayList<>();

        for (int x = playerX - radius; x <= playerX + radius; x++) {
            for (int z = playerZ - radius; z <= playerZ + radius; z++) {
                if (world.isChunkLoaded(x, z)) {
                    int count = entityRegistry.getEntityCount(world.getName(), x, z);
                    if (count >= entityThreshold) {
                        int distance = Math.max(Math.abs(x - playerX), Math.abs(z - playerZ));
                        laggyChunks.add(new ChunkInfo(x, z, count, distance));
                    }
                }
            }
        }

        sendChunkFinderResults(player, laggyChunks, radius);
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
