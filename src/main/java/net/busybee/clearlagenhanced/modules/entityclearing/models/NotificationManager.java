package net.busybee.clearlagenhanced.modules.entityclearing.models;

import net.busybee.clearlagenhanced.core.module.Module;
import net.busybee.clearlagenhanced.utils.MessageUtils;

import java.util.List;
import java.util.Map;

public class NotificationManager {

    private final Module module;

    public NotificationManager(Module module) {
        this.module = module;
    }

    public void sendClearWarnings(int secondsRemaining) {
        if (secondsRemaining <= 0) return;

        List<Integer> broadcastTimes = module.getConfig().getIntegerList("notifications.broadcast-times");
        if (broadcastTimes.isEmpty()) return;

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
