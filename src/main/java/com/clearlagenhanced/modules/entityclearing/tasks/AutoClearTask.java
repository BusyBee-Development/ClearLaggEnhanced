
package com.clearlagenhanced.modules.entityclearing.tasks;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.modules.entityclearing.models.AdaptiveIntervalSettings;
import com.clearlagenhanced.modules.entityclearing.models.EntityManager;
import com.clearlagenhanced.modules.entityclearing.models.NotificationManager;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoClearTask {
    private final ClearLaggEnhanced plugin;
    private final EntityManager entityManager;
    private final NotificationManager notificationManager;
    private final int defaultInterval;
    private final AdaptiveIntervalSettings adaptiveIntervalSettings;
    private final Set<String> trackedWorlds;

    @Getter
    private WrappedTask task;

    private final AtomicInteger remainingTime;
    private volatile StatusSnapshot statusSnapshot;

    public AutoClearTask(
            ClearLaggEnhanced plugin,
            EntityManager entityManager,
            NotificationManager notificationManager,
            int defaultInterval,
            AdaptiveIntervalSettings adaptiveIntervalSettings,
            List<String> trackedWorlds
    ) {
        this.plugin = plugin;
        this.entityManager = entityManager;
        this.notificationManager = notificationManager;
        this.defaultInterval = defaultInterval;
        this.adaptiveIntervalSettings = adaptiveIntervalSettings;
        this.trackedWorlds = new HashSet<>(trackedWorlds);
        this.remainingTime = new AtomicInteger(defaultInterval);
        this.statusSnapshot = new StatusSnapshot(
                adaptiveIntervalSettings.enabled(),
                adaptiveIntervalSettings.metric(),
                -1,
                defaultInterval,
                defaultInterval
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
        remainingTime.set(resolveNextInterval().activeIntervalSeconds());

        task = ClearLaggEnhanced.scheduler().runTimerAsync(() -> {
            try {
                int timeLeft = remainingTime.decrementAndGet();

                if (timeLeft <= 0) {
                    int cleared = entityManager.clearEntities();
                    if (cleared != -1) {
                        notificationManager.sendClearComplete(cleared);
                    }
                    remainingTime.set(resolveNextInterval().activeIntervalSeconds());
                } else {
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
            ClearLaggEnhanced.scheduler().cancelTask(task);
            task = null;
        }
    }

    private @NotNull StatusSnapshot resolveNextInterval() {
        if (!adaptiveIntervalSettings.enabled()) {
            StatusSnapshot fixedStatus = new StatusSnapshot(false, adaptiveIntervalSettings.metric(), -1, defaultInterval, defaultInterval);
            statusSnapshot = fixedStatus;
            return fixedStatus;
        }

        int metricValue = sampleMetricValue();
        int interval = adaptiveIntervalSettings.resolveInterval(metricValue, defaultInterval);
        StatusSnapshot adaptiveStatus = new StatusSnapshot(true, adaptiveIntervalSettings.metric(), metricValue, interval, defaultInterval);
        statusSnapshot = adaptiveStatus;
        return adaptiveStatus;
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

    public record StatusSnapshot(
            boolean adaptiveEnabled,
            @NotNull AdaptiveIntervalSettings.Metric metric,
            int sampledMetricValue,
            int activeIntervalSeconds,
            int fallbackIntervalSeconds
    ) {
    }
}
