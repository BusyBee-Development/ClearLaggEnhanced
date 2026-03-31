package com.clearlagenhanced.modules.entityclearing.models;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.utils.MessageUtils;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;

public class NotificationManager {

    private final ClearLaggEnhanced plugin;
    private final Module module;
    private final PlatformScheduler scheduler;

    public NotificationManager(ClearLaggEnhanced plugin, Module module) {
        this.plugin = plugin;
        this.module = module;
        this.scheduler = ClearLaggEnhanced.scheduler();
    }

    public void sendClearWarnings(int secondsRemaining) {
        if (secondsRemaining <= 0) return;

        List<Integer> broadcastTimes = module.getConfig().getIntegerList("notifications.broadcast-times");
        if (broadcastTimes == null || broadcastTimes.isEmpty()) return;

        if (broadcastTimes.contains(secondsRemaining)) {
            boolean toConsole = module.getConfig().getBoolean("notifications.broadcast-to-console", false);
            boolean toPlayers = module.getConfig().getBoolean("notifications.broadcast-to-players", true);

            MessageUtils.broadcastMessage(
                    "warnings.entity-clear",
                    Map.of("seconds", String.valueOf(secondsRemaining)),
                    toConsole,
                    toPlayers
            );
        }
    }

    public void sendClearComplete(int count) {
        boolean toConsole = module.getConfig().getBoolean("notifications.broadcast-to-console", false);
        boolean toPlayers = module.getConfig().getBoolean("notifications.broadcast-to-players", true);

        MessageUtils.broadcastMessage(
                "notifications.clear-complete",
                Map.of("count", String.valueOf(count), "time", "0"),
                toConsole,
                toPlayers
        );
    }

    public void shutdown() {
    }
}