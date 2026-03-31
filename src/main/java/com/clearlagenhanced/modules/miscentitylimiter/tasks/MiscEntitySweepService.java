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
    private final boolean protectNamed;
    private final Set<String> protectedTags = new HashSet<>();

    public MiscEntitySweepService(@NotNull ClearLaggEnhanced plugin, @NotNull Module module) {
        this.plugin = plugin;
        this.module = module;

        loadCaps(module.getConfig().getConfigurationSection("limits-per-chunk"));
        worldFilter.addAll(module.getConfig().getStringList("worlds"));
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
            sweepTask = ClearLaggEnhanced.scheduler().runTimer(() -> {
                for (World world : Bukkit.getWorlds()) {
                    if (!worldFilter.isEmpty() && !worldFilter.contains(world.getName())) continue;
                    for (Chunk chunk : world.getLoadedChunks()) {
                        processChunk(chunk);
                    }
                }
            }, intervalTicks, intervalTicks);
        }
    }

    private void processChunk(Chunk chunk) {
        if (caps.isEmpty()) return;

        Map<EntityType, List<Entity>> entitiesByType = new EnumMap<>(EntityType.class);
        for (Entity entity : chunk.getEntities()) {
            if (caps.containsKey(entity.getType()) && !isExempt(entity)) {
                entitiesByType.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
            }
        }

        for (Map.Entry<EntityType, List<Entity>> entry : entitiesByType.entrySet()) {
            EntityType type = entry.getKey();
            List<Entity> list = entry.getValue();
            int cap = caps.get(type);

            if (list.size() > cap) {
                int toRemove = list.size() - cap;
                for (int i = 0; i < toRemove; i++) {
                    Entity entity = list.get(i);
                    ClearLaggEnhanced.scheduler().runAtEntity(entity, task -> entity.remove());
                }
                notifyAdmins(chunk, type, toRemove, false);
            }
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
}
