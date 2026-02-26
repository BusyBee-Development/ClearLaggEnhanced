package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.utils.MessageUtils;
import com.tcoded.folialib.impl.PlatformScheduler;
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

    private final ConfigManager configManager;
    private final PlatformScheduler scheduler;
    private final boolean isPaperServer;

    public PerformanceManager(@NotNull ClearLaggEnhanced plugin) {
        this.configManager = plugin.getConfigManager();
        this.scheduler = ClearLaggEnhanced.scheduler();
        this.isPaperServer = checkPaperServer();
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

    public long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
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

    public int getTotalEntities() {
        int total = 0;
        for (World world : Bukkit.getWorlds()) {
            total += world.getEntities().size();
        }

        return total;
    }

    public boolean isServerLagging() {
        return getTPS() < 18.0;
    }

    public boolean isMemoryHigh() {
        return getMemoryUsagePercentage() > 85.0;
    }

    public void findLaggyChunksAsync(@NotNull Player player) {
        MessageUtils.sendMessage(player, "chunkfinder.scanning");

        World world = player.getWorld();
        int radius = configManager.getInt("chunk-finder.radius", 10);
        int entityThreshold = configManager.getInt("chunk-finder.entity-threshold", 50);

        Chunk playerChunk = player.getLocation().getChunk();
        int playerX = playerChunk.getX();
        int playerZ = playerChunk.getZ();

        List<ChunkInfo> laggyChunks = Collections.synchronizedList(new ArrayList<>());
        List<int[]> targets = new ArrayList<>();

        for (int x = playerX - radius; x <= playerX + radius; x++) {
            for (int z = playerZ - radius; z <= playerZ + radius; z++) {
                if (world.isChunkLoaded(x, z)) {
                    targets.add(new int[]{x, z});
                }
            }
        }

        if (targets.isEmpty()) {
            Map<String, String> ph = new ConcurrentHashMap<>();
            ph.put("radius", String.valueOf(radius));
            MessageUtils.sendMessage(player, "chunkfinder.none-found", ph);
            return;
        }

        final int BATCH_SIZE = 25;
        final List<List<int[]>> batches = new ArrayList<>();
        for (int i = 0; i < targets.size(); i += BATCH_SIZE) {
            batches.add(targets.subList(i, Math.min(i + BATCH_SIZE, targets.size())));
        }

        AtomicInteger pending = new AtomicInteger(batches.size());

        for (List<int[]> batch : batches) {
            if (batch.isEmpty()) {
                pending.decrementAndGet();
                continue;
            }

            int[] firstCoord = batch.get(0);
            Location location = new Location(world, (firstCoord[0] << 4), 0, (firstCoord[1] << 4));

            scheduler.runAtLocation(location, task -> {
                for (int[] coord : batch) {
                    int cx = coord[0];
                    int cz = coord[1];

                    if (!world.isChunkLoaded(cx, cz)) {
                        continue;
                    }

                    Chunk chunk = world.getChunkAt(cx, cz);
                    Entity[] entities = chunk.getEntities();

                    if (entities.length >= entityThreshold) {
                        int distance = Math.max(Math.abs(cx - playerX), Math.abs(cz - playerZ));
                        laggyChunks.add(new ChunkInfo(cx, cz, entities.length, distance));
                    }
                }

                if (pending.decrementAndGet() == 0) {
                    // Schedule on the player's entity thread, not the global tick thread
                    scheduler.runAtEntity(player, task1 -> {
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
            });
        }
    }

    private record ChunkInfo(int x, int z, int entityCount, int distance) {
    }
}
