package com.clearlagenhanced.modules.miscentitylimiter.tasks;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MiscEntitySweepService {

    private final ClearLaggEnhanced plugin;
    private final Module module;
    private WrappedTask sweepTask;
    private final Map<EntityType, Integer> caps = new EnumMap<>(EntityType.class);
    private final Set<String> worldFilter = new HashSet<>();
    private final int maxChunksPerTick;
    private final boolean protectNamed;
    private final Set<String> protectedTags = new HashSet<>();
    private final List<ChunkRef> pendingChunks = new ArrayList<>();
    private int chunkCursor;

    public MiscEntitySweepService(@NotNull ClearLaggEnhanced plugin, @NotNull Module module) {
        this.plugin = plugin;
        this.module = module;

        loadCaps(module.getConfig().getConfigurationSection("limits-per-chunk"));
        worldFilter.addAll(module.getConfig().getStringList("worlds"));
        maxChunksPerTick = Math.max(1, module.getConfig().getInt("sweep.max-chunks-per-tick", 20));
        protectNamed = module.getConfig().getBoolean("protect.named", true);
        protectedTags.addAll(module.getConfig().getStringList("protect.tags"));
    }

    private void loadCaps(@Nullable ConfigurationSection sec) {
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                EntityType type = EntityType.valueOf(key.toUpperCase(Locale.ROOT));
                int cap = sec.getInt(key, -1);
                if (cap >= 0) caps.put(type, cap);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void start() {
        int intervalTicks = module.getConfig().getInt("sweep.interval-ticks", 100);
        if (intervalTicks > 0) {
            sweepTask = ClearLaggEnhanced.scheduler().runTimer(this::runSweepTick, intervalTicks, intervalTicks);
        }
    }

    private void runSweepTick() {
        if (caps.isEmpty()) {
            return;
        }

        if (chunkCursor >= pendingChunks.size()) {
            rebuildPendingChunks();
        }

        int processedChunks = 0;
        while (processedChunks < maxChunksPerTick && chunkCursor < pendingChunks.size()) {
            ChunkRef chunkRef = pendingChunks.get(chunkCursor++);
            World world = Bukkit.getWorld(chunkRef.worldName());
            if (world == null || !world.isChunkLoaded(chunkRef.x(), chunkRef.z())) {
                continue;
            }

            processChunk(world.getChunkAt(chunkRef.x(), chunkRef.z()));
            processedChunks++;
        }

        if (chunkCursor >= pendingChunks.size()) {
            pendingChunks.clear();
            chunkCursor = 0;
        }
    }

    private void rebuildPendingChunks() {
        pendingChunks.clear();
        chunkCursor = 0;

        for (World world : Bukkit.getWorlds()) {
            if (!worldFilter.isEmpty() && !worldFilter.contains(world.getName())) {
                continue;
            }

            for (Chunk chunk : world.getLoadedChunks()) {
                pendingChunks.add(new ChunkRef(world.getName(), chunk.getX(), chunk.getZ()));
            }
        }
    }

    private void processChunk(Chunk chunk) {
        if (caps.isEmpty()) return;

        Entity[] entities = chunk.getEntities();
        Map<EntityType, Integer> countsByType = new EnumMap<>(EntityType.class);
        for (Entity entity : entities) {
            if (caps.containsKey(entity.getType()) && !isExempt(entity)) {
                countsByType.merge(entity.getType(), 1, Integer::sum);
            }
        }

        Map<EntityType, Integer> removalsByType = new EnumMap<>(EntityType.class);
        for (Map.Entry<EntityType, Integer> entry : countsByType.entrySet()) {
            int cap = caps.getOrDefault(entry.getKey(), -1);
            int toRemove = entry.getValue() - cap;
            if (toRemove > 0) {
                removalsByType.put(entry.getKey(), toRemove);
            }
        }

        if (removalsByType.isEmpty()) {
            return;
        }

        Map<EntityType, Integer> removedByType = new EnumMap<>(EntityType.class);
        for (Entity entity : entities) {
            EntityType type = entity.getType();
            Integer remaining = removalsByType.get(type);
            if (remaining == null || remaining <= 0 || isExempt(entity)) {
                continue;
            }

            removedByType.merge(type, 1, Integer::sum);
            removalsByType.put(type, remaining - 1);
            ClearLaggEnhanced.scheduler().runAtEntity(entity, task -> entity.remove());
        }

        for (Map.Entry<EntityType, Integer> entry : removedByType.entrySet()) {
            notifyAdmins(chunk, entry.getKey(), entry.getValue(), false);
        }
    }

    private boolean isExempt(Entity entity) {
        if (protectNamed) {
            if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) return true;
            if (entity instanceof Item item) {
                ItemStack stack = item.getItemStack();
                if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) return true;
            }
        }
        if (!protectedTags.isEmpty()) {
            for (String tag : protectedTags) {
                if (entity.getScoreboardTags().contains(tag)) return true;
            }
        }
        return false;
    }

    public void shutdown() {
        if (sweepTask != null) {
            ClearLaggEnhanced.scheduler().cancelTask(sweepTask);
            sweepTask = null;
        }
    }

    public void notifyAdmins(@NotNull Chunk chunk, @NotNull EntityType type, int removed, boolean prevented) {
    }

    private record ChunkRef(@NotNull String worldName, int x, int z) {
    }
}
