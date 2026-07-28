package net.busybee.clearlaggenhanced.modules.performance.models;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalEntityRegistry implements Listener {
    private final ClearLaggEnhanced plugin;
    private final Map<ChunkKey, AtomicInteger> chunkEntityCounts = new ConcurrentHashMap<>();
    private final AtomicInteger globalEntityCount = new AtomicInteger(0);

    public GlobalEntityRegistry(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
    }

    public void init() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        try {
            Class.forName("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.MONITOR)
                public void onEntityRemove(com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent event) {
                    handleEntityRemoval(event.getEntity());
                }
            }, plugin);
        } catch (ClassNotFoundException ignored) {}

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                int count = chunk.getEntities().length;
                chunkEntityCounts.put(new ChunkKey(world.getName(), chunk.getX(), chunk.getZ()), new AtomicInteger(count));
                globalEntityCount.addAndGet(count);
            }
        }
    }

    public int getGlobalEntityCount() {
        return globalEntityCount.get();
    }

    public int getEntityCount(Chunk chunk) {
        return getEntityCount(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public int getEntityCount(String world, int x, int z) {
        AtomicInteger count = chunkEntityCounts.get(new ChunkKey(world, x, z));
        return count != null ? count.get() : 0;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        Location loc = entity.getLocation();
        ChunkKey key = new ChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        
        chunkEntityCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        globalEntityCount.incrementAndGet();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        try {
            Class.forName("com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent");
        } catch (ClassNotFoundException e) {
            handleEntityRemoval(event.getEntity());
        }
    }

    private void handleEntityRemoval(Entity entity) {
        Location loc = entity.getLocation();
        ChunkKey key = new ChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        
        AtomicInteger count = chunkEntityCounts.get(key);
        if (count != null && count.get() > 0) {
            count.decrementAndGet();
            globalEntityCount.decrementAndGet();
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        
        Chunk chunk = event.getChunk();
        int count = chunk.getEntities().length;
        chunkEntityCounts.put(new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()), new AtomicInteger(count));
        globalEntityCount.addAndGet(count);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        ChunkKey key = new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        AtomicInteger count = chunkEntityCounts.remove(key);
        if (count != null) {
            globalEntityCount.addAndGet(-count.get());
        }
    }

    private record ChunkKey(String world, int x, int z) {}
}
