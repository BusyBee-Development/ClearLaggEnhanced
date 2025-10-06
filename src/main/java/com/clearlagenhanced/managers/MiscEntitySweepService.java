package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MiscEntitySweepService {

    private final ClearLaggEnhanced plugin;
    private final PlatformScheduler scheduler;
    private final Map<EntityType, Integer> caps;
    private final Set<String> worldFilter;
    private final boolean protectNamed;
    private final Set<String> protectedTags;
    private final int maxChunksPerTick;
    private final long intervalTicks;
    private final long notifyThrottleTicks;
    private final String adminPerm;

    private WrappedTask sweepTask;
    private final AtomicInteger cursor = new AtomicInteger(0);
    private final Map<String, Long> lastNotifyTick = new ConcurrentHashMap<>();

    public MiscEntitySweepService(@NotNull ClearLaggEnhanced plugin, @NotNull ConfigManager cfg) {
        this.plugin = plugin;
        this.scheduler = ClearLaggEnhanced.scheduler();

        this.caps = loadCaps(cfg);
        this.worldFilter = new HashSet<>(cfg.getStringList("lag-prevention.misc-entity-limiter.worlds"));
        this.protectNamed = cfg.getBoolean("lag-prevention.misc-entity-limiter.protect.named", true);
        this.protectedTags = new HashSet<>(cfg.getStringList("lag-prevention.misc-entity-limiter.protect.tags"));
        this.maxChunksPerTick = Math.max(1, cfg.getInt("lag-prevention.misc-entity-limiter.sweep.max-chunks-per-tick", 20));
        this.intervalTicks = Math.max(1, cfg.getInt("lag-prevention.misc-entity-limiter.sweep.interval-ticks", 100));
        this.adminPerm = cfg.getString("lag-prevention.misc-entity-limiter.notify.admins-permission", "CLE.admin");
        long throttleSeconds = Math.max(5, cfg.getInt("lag-prevention.misc-entity-limiter.notify.throttle-seconds", 60));
        this.notifyThrottleTicks = throttleSeconds * 20L;
    }

    private Map<EntityType, Integer> loadCaps(@NotNull ConfigManager cfg) {
        Map<EntityType, Integer> map = new EnumMap<>(EntityType.class);
        var sec = cfg.getConfig().getConfigurationSection("lag-prevention.misc-entity-limiter.limits-per-chunk");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase(Locale.ROOT));
                    int cap = sec.getInt(key, -1);
                    if (cap >= 0) {
                        map.put(type, cap);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return map;
    }

    public void start() {
        if (sweepTask != null) {
            return;
        }

        sweepTask = scheduler.runTimer(this::tick, intervalTicks, intervalTicks);
    }

    public void shutdown() {
        if (sweepTask != null) {
            scheduler.cancelTask(sweepTask);
            sweepTask = null;
        }

        lastNotifyTick.clear();
    }

    private boolean isWorldAllowed(@NotNull World world) {
        return worldFilter.isEmpty() || worldFilter.contains(world.getName());
    }

    private boolean exempt(@NotNull Entity e) {
        if (protectNamed && e.getCustomName() != null && !e.getCustomName().isEmpty()) {
            return true;
        }

        if (!protectedTags.isEmpty()) {
            for (String t : protectedTags) if (e.getScoreboardTags().contains(t)) return true;
        }

        return false;
    }

    private void tick() {
        List<World> worlds = plugin.getServer().getWorlds();
        List<Chunk> chunks = new ArrayList<>();
        for (World w : worlds) {
            if (!isWorldAllowed(w)) {
                continue;
            }

            chunks.addAll(Arrays.asList(w.getLoadedChunks()));
        }

        if (chunks.isEmpty()) {
            return;
        }

        int size = chunks.size();
        for (int processed = 0; processed < maxChunksPerTick; processed++) {
            int i = Math.floorMod(cursor.getAndIncrement(), size);
            Chunk chunk = chunks.get(i);
            scheduler.runAtLocation(chunk.getBlock(0, 0, 0).getLocation(), task -> enforceChunk(chunk));
        }
    }

    private void enforceChunk(Chunk chunk) {
        chunk.getWorld().getFullTime();
        Map<EntityType, List<Entity>> byType = new EnumMap<>(EntityType.class);
        for (Entity entity : chunk.getEntities()) {
            if (!caps.containsKey(entity.getType())) {
                continue;
            }

            byType.computeIfAbsent(entity.getType(), k -> new ArrayList<>()).add(entity);
        }

        for (Map.Entry<EntityType, List<Entity>> entry : byType.entrySet()) {
            EntityType type = entry.getKey();
            int cap = caps.getOrDefault(type, -1);
            if (cap < 0) {
                continue;
            }

            List<Entity> list = entry.getValue();
            List<Entity> candidates = new ArrayList<>();
            for (Entity e : list) {
                if (!exempt(e)) candidates.add(e);
            }

            int over = candidates.size() - cap;
            if (over <= 0) {
                continue;
            }

            candidates.sort(this::compareForRemoval);

            AtomicInteger remaining = new AtomicInteger(over);
            AtomicInteger scheduled = new AtomicInteger(0);
            AtomicInteger removedCount = new AtomicInteger(0);

            for (Entity victim : candidates) {
                if (remaining.get() <= 0) {
                    break;
                }

                scheduled.incrementAndGet();
                final AtomicBoolean once = new AtomicBoolean(false);

                scheduler.runAtEntity(victim, t -> {
                    if (!once.compareAndSet(false, true)) {
                        return;
                    }

                    int before = remaining.getAndUpdate(curr -> curr > 0 ? curr - 1 : curr);
                    if (before <= 0) {
                        if (scheduled.decrementAndGet() == 0 && removedCount.get() > 0) {
                            notifyAdmins(chunk, type, removedCount.get(), false);
                        }

                        return;
                    }

                    boolean removed = false;
                    if (!victim.isDead() && victim.getType() == type && !exempt(victim)) {
                        victim.remove();
                        removed = true;
                    }

                    if (removed) {
                        removedCount.incrementAndGet();
                    } else {
                        remaining.incrementAndGet();
                    }

                    if (scheduled.decrementAndGet() == 0 && removedCount.get() > 0) {
                        notifyAdmins(chunk, type, removedCount.get(), false);
                    }
                });
            }
        }
    }

    private int compareForRemoval(Entity a, Entity b) {
        boolean aNamed = a.getCustomName() != null && !a.getCustomName().isEmpty();
        boolean bNamed = b.getCustomName() != null && !b.getCustomName().isEmpty();
        if (aNamed != bNamed) {
            return aNamed ? 1 : -1;
        }

        double ap = nearestPlayerDistSq(a);
        double bp = nearestPlayerDistSq(b);
        int cmp = Double.compare(bp, ap);
        if (cmp != 0) {
            return cmp;
        }

        return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
    }

    private double nearestPlayerDistSq(@NotNull Entity entity) {
        double min = Double.POSITIVE_INFINITY;
        for (Player player : entity.getWorld().getPlayers()) {
            double d = player.getLocation().distanceSquared(entity.getLocation());
            if (d < min) {
                min = d;
            }
        }

        if (Double.isInfinite(min)) {
            return 1.0E12;
        }

        return min;
    }

    public void notifyAdmins(@NotNull Chunk chunk, @NotNull EntityType type, int count, boolean blocked) {
        long nowTick = chunk.getWorld().getFullTime();
        final String nkey = chunk.getWorld().getName() + ":" + chunk.getX() + "," + chunk.getZ() + ":" + type.name();

        Long prev = lastNotifyTick.get(nkey);
        if (prev != null && (nowTick - prev) < notifyThrottleTicks) {
            return;
        }

        lastNotifyTick.put(nkey, nowTick);

        Map<String, String> ph = new ConcurrentHashMap<>();
        ph.put("x", String.valueOf(chunk.getX()));
        ph.put("z", String.valueOf(chunk.getZ()));
        ph.put("type", type.name());
        ph.put("removed", String.valueOf(count));
        String msgKey = blocked ? "misc-entity-limiter.blocked" : "misc-entity-limiter.trimmed";

        var mm = plugin.getMessageManager();
        var component = mm.getMessage(msgKey, ph);
        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(adminPerm))
                .forEach(p -> p.sendMessage(component));
    }
}
