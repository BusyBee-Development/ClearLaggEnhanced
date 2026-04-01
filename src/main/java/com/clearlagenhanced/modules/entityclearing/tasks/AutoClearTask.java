
package com.clearlagenhanced.modules.entityclearing.tasks;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.modules.entityclearing.models.EntityManager;
import com.clearlagenhanced.modules.entityclearing.models.NotificationManager;
import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;


public class AutoClearTask {
    private final ClearLaggEnhanced plugin;
    private final EntityManager entityManager;
    private final NotificationManager notificationManager;
    private final int clearInterval;

    @Getter
    private WrappedTask task;

    private final AtomicInteger remainingTime;

    public AutoClearTask(ClearLaggEnhanced plugin, EntityManager entityManager, NotificationManager notificationManager, int clearInterval, int warnLead) {
        this.plugin = plugin;
        this.entityManager = entityManager;
        this.notificationManager = notificationManager;
        this.clearInterval = clearInterval;
        this.remainingTime = new AtomicInteger(clearInterval);
    }

    public int getRemainingTime() {
        return remainingTime.get();
    }

    public void start() {
        stop();
        task = ClearLaggEnhanced.scheduler().runTimerAsync(() -> {
            try {
                int timeLeft = remainingTime.decrementAndGet();

                if (timeLeft <= 0) {
                    int cleared = entityManager.clearEntities();
                    if (cleared != -1) {
                        notificationManager.sendClearComplete(cleared);
                    }
                    remainingTime.set(clearInterval);
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
}
