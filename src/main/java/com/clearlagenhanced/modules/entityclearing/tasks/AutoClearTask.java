
package com.clearlagenhanced.modules.entityclearing.tasks;

import com.clearlagenhanced.ClearLaggEnhanced;

import com.clearlagenhanced.modules.entityclearing.models.EntityManager;

import com.clearlagenhanced.modules.entityclearing.models.NotificationManager;

import com.tcoded.folialib.wrapper.task.WrappedTask;

import lombok.Getter;


public class AutoClearTask {
    private final ClearLaggEnhanced plugin;
    private final EntityManager entityManager;
    private final NotificationManager notificationManager;
    private final int clearInterval;

    @Getter
    private WrappedTask task;

    @Getter
    private volatile int remainingTime;

    public AutoClearTask(ClearLaggEnhanced plugin, EntityManager entityManager, NotificationManager notificationManager, int clearInterval, int warnLead) {
        this.plugin = plugin;
        this.entityManager = entityManager;
        this.notificationManager = notificationManager;
        this.clearInterval = clearInterval;
        this.remainingTime = clearInterval;
    }

    public void start() {
        stop();
        task = ClearLaggEnhanced.scheduler().runTimer(() -> {
            try {
                remainingTime--;

                if (remainingTime <= 0) {
                    int cleared = entityManager.clearEntities();
                    notificationManager.sendClearComplete(cleared);
                    remainingTime = clearInterval;
                } else {
                    notificationManager.sendClearWarnings(remainingTime);
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