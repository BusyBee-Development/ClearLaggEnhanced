package net.busybee.clearlaggenhanced.modules.entityclearing.models;
        
import com.cryptomorin.xseries.XSound;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
            sendNotification("warnings.entity-clear", Map.of("seconds", String.valueOf(secondsRemaining)));
            playNotificationSound();
        }
    }

    public void sendClearComplete(int count) {
        sendNotification("notifications.clear-complete", Map.of("count", String.valueOf(count), "time", "0"));
        playNotificationSound();
    }

    private void sendNotification(String path, Map<String, String> placeholders) {
        boolean toConsole = module.getConfig().getBoolean("notifications.console-notifications", false);
        List<String> types = module.getConfig().getStringList("notifications.types");

        if (toConsole) {
            MessageUtils.broadcastMessage(path, placeholders, true, false);
        }

        if (types.isEmpty()) {
            MessageUtils.broadcastMessage(path, placeholders, false, true);
            return;
        }

        for (String type : types) {
            switch (type.toUpperCase()) {
                case "CHAT" -> MessageUtils.broadcastMessage(path, placeholders, false, true);
                case "ACTION_BAR" -> MessageUtils.broadcastActionBar(path, placeholders);
            }
        }
    }

    private void playNotificationSound() {
        if (!module.getConfig().getBoolean("notifications.sound.enabled", false)) return;

        String soundName = module.getConfig().getString("notifications.sound.name", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) module.getConfig().getDouble("notifications.sound.volume", 1.0);
        float pitch = (float) module.getConfig().getDouble("notifications.sound.pitch", 1.0);

        XSound.matchXSound(soundName).ifPresent(xSound -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                xSound.play(player, volume, pitch);
            }
        });
    }

    public void shutdown() {
    }
}
