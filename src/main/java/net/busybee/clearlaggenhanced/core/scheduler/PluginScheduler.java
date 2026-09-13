package net.busybee.clearlaggenhanced.core.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Wraps Paper's native region-based scheduler API (io.papermc.paper.threadedregions.scheduler.*).
 * That API has shipped in paper-api since 1.20.1 and runs correctly on regular Paper, Folia,
 * and Folia forks (e.g. Canvas) alike, so no third-party scheduler abstraction is required.
 */
public class PluginScheduler {

    private final Plugin plugin;

    public PluginScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public ScheduledTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(),
                Math.max(1, delayTicks), Math.max(1, periodTicks));
    }

    public ScheduledTask runTimerAsync(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(),
                Math.max(1, delayTicks) * 50L, Math.max(1, periodTicks) * 50L, TimeUnit.MILLISECONDS);
    }

    public ScheduledTask runLater(Runnable task, long delayTicks) {
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), Math.max(1, delayTicks));
    }

    public ScheduledTask runNextTick(Consumer<ScheduledTask> task) {
        return Bukkit.getGlobalRegionScheduler().run(plugin, task);
    }

    public ScheduledTask runAsync(Consumer<ScheduledTask> task) {
        return Bukkit.getAsyncScheduler().runNow(plugin, task);
    }

    public ScheduledTask runAtEntity(Entity entity, Consumer<ScheduledTask> task) {
        return entity.getScheduler().run(plugin, task, null);
    }

    public ScheduledTask runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        return entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), null,
                Math.max(1, delayTicks), Math.max(1, periodTicks));
    }

    public ScheduledTask runAtLocation(Location location, Consumer<ScheduledTask> task) {
        return Bukkit.getRegionScheduler().run(plugin, location, task);
    }

    public static void cancelTask(ScheduledTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
