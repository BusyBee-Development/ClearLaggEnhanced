
package net.busybee.clearlaggenhanced.modules.entityclearing.tasks;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.scheduler.PluginScheduler;
import net.busybee.clearlaggenhanced.modules.entityclearing.models.AdaptiveIntervalSettings;
import net.busybee.clearlaggenhanced.modules.entityclearing.models.EntityManager;
import net.busybee.clearlaggenhanced.modules.entityclearing.models.NotificationManager;
import net.busybee.clearlaggenhanced.modules.entityclearing.models.PerformanceGateSettings;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoClearTask {

    private static final long HEARTBEAT_STALE_NANOS = TimeUnit.SECONDS.toNanos(3);

    private final ClearLaggEnhanced plugin;
    private final EntityManager entityManager;
    private final NotificationManager notificationManager;
    private final int defaultInterval;
    private final AdaptiveIntervalSettings adaptiveIntervalSettings;
    private final PerformanceGateSettings performanceGateSettings;
    private final Set<String> trackedWorlds;
    private final @Nullable Method averageTickTimeMethod;

    @Getter
    private ScheduledTask task;

    private final AtomicInteger remainingTime;
    private volatile StatusSnapshot statusSnapshot;
    private volatile long thresholdBreachedSinceMillis = -1L;
    private volatile boolean averageTickTimeUnavailableLogged;
    private final boolean isFolia = PluginScheduler.isFolia();
    private ScheduledTask heartbeatTask;
    private volatile long lastHeartbeatNanos = System.nanoTime();

    public AutoClearTask(
            ClearLaggEnhanced plugin,
            EntityManager entityManager,
            NotificationManager notificationManager,
            int defaultInterval,
            AdaptiveIntervalSettings adaptiveIntervalSettings,
            PerformanceGateSettings performanceGateSettings,
            List<String> trackedWorlds
    ) {
        this.plugin = plugin;
        this.entityManager = entityManager;
        this.notificationManager = notificationManager;
        this.defaultInterval = defaultInterval;
        this.adaptiveIntervalSettings = adaptiveIntervalSettings;
        this.performanceGateSettings = performanceGateSettings;
        this.trackedWorlds = new HashSet<>(trackedWorlds);
        this.averageTickTimeMethod = resolveAverageTickTimeMethod();
        this.remainingTime = new AtomicInteger(defaultInterval);
        this.statusSnapshot = new StatusSnapshot(
                adaptiveIntervalSettings.enabled(),
                adaptiveIntervalSettings.metric(),
                -1,
                defaultInterval,
                defaultInterval,
                createInitialPerformanceGateStatus()
        );
    }

    public int getRemainingTime() {
        return remainingTime.get();
    }

    public @Nullable StatusSnapshot getStatusSnapshot() {
        return statusSnapshot;
    }

    public void start() {
        stop();
        remainingTime.set(resolveNextInterval(updatePerformanceGateStatus()).activeIntervalSeconds());

        lastHeartbeatNanos = System.nanoTime();
        heartbeatTask = ClearLaggEnhanced.scheduler().runTimer(() -> lastHeartbeatNanos = System.nanoTime(), 1L, 1L);

        task = ClearLaggEnhanced.scheduler().runTimerAsync(() -> {
            try {
                // The timer runs on wall-clock time, so it keeps firing while the server is paused
                // (vanilla pause-when-empty-seconds). Hold the countdown until ticking resumes.
                if (isServerPaused()) {
                    return;
                }

                PerformanceGateStatus performanceGateStatus = updatePerformanceGateStatus();
                int timeLeft = remainingTime.updateAndGet(current -> current > 0 ? current - 1 : 0);

                if (timeLeft <= 0) {
                    if (performanceGateStatus.blockingClears()) {
                        updateStatusSnapshot(performanceGateStatus);
                        return;
                    }

                    long clearStart = System.currentTimeMillis();
                    int cleared = entityManager.clearEntities();
                    if (cleared != -1) {
                        notificationManager.sendClearComplete(cleared, System.currentTimeMillis() - clearStart);
                    }
                    remainingTime.set(resolveNextInterval(performanceGateStatus).activeIntervalSeconds());
                } else {
                    updateStatusSnapshot(performanceGateStatus);
                    notificationManager.sendClearWarnings(timeLeft);
                }
            } catch (Throwable t) {
                plugin.getLogger().severe("Error in entity clearing timer task: " + t.getMessage());
                t.printStackTrace();
            }
        }, 20L, 20L);
    }
    
    public void stop() {
        if (task != null) {
            PluginScheduler.cancelTask(task);
            task = null;
        }
        if (heartbeatTask != null) {
            PluginScheduler.cancelTask(heartbeatTask);
            heartbeatTask = null;
        }
    }

    private boolean isServerPaused() {
        // Bukkit.getCurrentTick() keeps advancing on Paper forks while paused, so it cannot be used here.
        // The global region scheduler, however, stops ticking - and every clear depends on it.
        return System.nanoTime() - lastHeartbeatNanos > HEARTBEAT_STALE_NANOS;
    }

    private @NotNull StatusSnapshot resolveNextInterval(@NotNull PerformanceGateStatus performanceGateStatus) {
        if (!adaptiveIntervalSettings.enabled()) {
            StatusSnapshot fixedStatus = new StatusSnapshot(
                    false,
                    adaptiveIntervalSettings.metric(),
                    -1,
                    defaultInterval,
                    defaultInterval,
                    performanceGateStatus
            );
            statusSnapshot = fixedStatus;
            return fixedStatus;
        }

        int metricValue = sampleMetricValue();
        int interval = adaptiveIntervalSettings.resolveInterval(metricValue, defaultInterval);
        StatusSnapshot adaptiveStatus = new StatusSnapshot(
                true,
                adaptiveIntervalSettings.metric(),
                metricValue,
                interval,
                defaultInterval,
                performanceGateStatus
        );
        statusSnapshot = adaptiveStatus;
        return adaptiveStatus;
    }

    private void updateStatusSnapshot(@NotNull PerformanceGateStatus performanceGateStatus) {
        StatusSnapshot currentStatus = statusSnapshot;
        statusSnapshot = new StatusSnapshot(
                currentStatus.adaptiveEnabled(),
                currentStatus.metric(),
                currentStatus.sampledMetricValue(),
                currentStatus.activeIntervalSeconds(),
                currentStatus.fallbackIntervalSeconds(),
                performanceGateStatus
        );
    }

    private int sampleMetricValue() {
        if (Bukkit.isPrimaryThread()) {
            return sampleMetricValueSync();
        }

        AtomicInteger sampledValue = new AtomicInteger(-1);
        CountDownLatch latch = new CountDownLatch(1);

        ClearLaggEnhanced.scheduler().runNextTick(task -> {
            try {
                sampledValue.set(sampleMetricValueSync());
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out while sampling entity clearing adaptive interval metric. Falling back to the base interval for this cycle.");
                return -1;
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return -1;
        }

        return sampledValue.get();
    }

    private int sampleMetricValueSync() {
        return switch (adaptiveIntervalSettings.metric()) {
            case ENTITY_COUNT -> countLoadedEntities();
            case PLAYER_COUNT -> Bukkit.getOnlinePlayers().size();
        };
    }

    private int countLoadedEntities() {
        int totalEntities = 0;
        for (World world : Bukkit.getWorlds()) {
            if (!trackedWorlds.isEmpty() && !trackedWorlds.contains(world.getName())) {
                continue;
            }

            totalEntities += world.getEntities().size();
        }

        return totalEntities;
    }

    private @NotNull PerformanceGateStatus createInitialPerformanceGateStatus() {
        if (!performanceGateSettings.enabled()) {
            return PerformanceGateStatus.disabled();
        }

        return new PerformanceGateStatus(true, false, false, -1.0D, performanceGateSettings.msptThreshold(), 0, performanceGateSettings.sustainedSeconds());
    }

    private @NotNull PerformanceGateStatus updatePerformanceGateStatus() {
        if (!performanceGateSettings.enabled()) {
            return PerformanceGateStatus.disabled();
        }

        Double currentAverageMspt = sampleAverageTickTime();
        if (currentAverageMspt == null) {
            return new PerformanceGateStatus(true, true, false, -1.0D, performanceGateSettings.msptThreshold(), 0, performanceGateSettings.sustainedSeconds());
        }

        long now = System.currentTimeMillis();
        if (currentAverageMspt >= performanceGateSettings.msptThreshold()) {
            if (thresholdBreachedSinceMillis < 0L) {
                thresholdBreachedSinceMillis = now;
            }
        } else {
            thresholdBreachedSinceMillis = -1L;
        }

        int sustainedForSeconds = thresholdBreachedSinceMillis < 0L
                ? 0
                : (int) ((now - thresholdBreachedSinceMillis) / 1000L);

        boolean thresholdSatisfied = thresholdBreachedSinceMillis >= 0L
                && sustainedForSeconds >= performanceGateSettings.sustainedSeconds();

        return new PerformanceGateStatus(
                true,
                false,
                !thresholdSatisfied,
                currentAverageMspt,
                performanceGateSettings.msptThreshold(),
                sustainedForSeconds,
                performanceGateSettings.sustainedSeconds()
        );
    }

    private @Nullable Method resolveAverageTickTimeMethod() {
        if (isFolia) {
            // Folia has no global MSPT.
            return null;
        }

        try {
            return Bukkit.getServer().getClass().getMethod("getAverageTickTime");
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private @Nullable Double sampleTps() {
        try {
            return Bukkit.getServer().getTPS()[0];
        } catch (Throwable ignored) {
            return null;
        }
    }

    private @Nullable Double sampleAverageTickTime() {
        if (averageTickTimeMethod == null) {
            Double tps = sampleTps();
            if (tps != null) {
                return 1000.0 / Math.max(0.1, tps);
            }

            if (!averageTickTimeUnavailableLogged) {
                if (isFolia) {
                    plugin.getLogger().info("Performance gate is active on Folia; note that metrics are limited in regionized environments.");
                } else {
                    plugin.getLogger().warning("Entity clearing performance gate is enabled, but performance metrics are unavailable. Continuing with normal clearing behavior.");
                }
                averageTickTimeUnavailableLogged = true;
            }
            return null;
        }

        try {
            Object value = averageTickTimeMethod.invoke(Bukkit.getServer());
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Exception exception) {
            if (!averageTickTimeUnavailableLogged) {
                Throwable cause = (exception instanceof java.lang.reflect.InvocationTargetException ite) ? ite.getCause() : exception;
                plugin.getLogger().warning("Entity clearing performance gate is not supported on this platform: " + (cause != null ? cause.getMessage() : "null"));
                averageTickTimeUnavailableLogged = true;
            }
        }

        return null;
    }

    public record StatusSnapshot(
            boolean adaptiveEnabled,
            @NotNull AdaptiveIntervalSettings.Metric metric,
            int sampledMetricValue,
            int activeIntervalSeconds,
            int fallbackIntervalSeconds,
            @NotNull PerformanceGateStatus performanceGateStatus
    ) {
    }

    public record PerformanceGateStatus(
            boolean enabled,
            boolean metricUnavailable,
            boolean blockingClears,
            double currentAverageMspt,
            double thresholdMspt,
            int sustainedForSeconds,
            int requiredSustainedSeconds
    ) {
        public static @NotNull PerformanceGateStatus disabled() {
            return new PerformanceGateStatus(false, false, false, -1.0D, -1.0D, 0, 0);
        }
    }
}
