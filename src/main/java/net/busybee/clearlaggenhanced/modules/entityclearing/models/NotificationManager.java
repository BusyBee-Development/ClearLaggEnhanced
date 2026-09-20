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
            sendNotification("warnings.entity-clear", Map.of("seconds", String.valueOf(secondsRemaining)), null);
            playNotificationSound(null);
        }
    }

    public void sendClearComplete(int count, long durationMs) {
        Map<String, String> placeholders = Map.of("count", String.valueOf(count), "time", String.valueOf(durationMs));
        sendNotification("notifications.clear-complete", placeholders, "clear-complete");
        playNotificationSound("clear-complete");
    }

    public void playClearCompleteSound(Player player) {
        if (player == null) return;
        playNotificationSound("clear-complete", player);
    }

    private void sendNotification(String path, Map<String, String> placeholders, String overrideKey) {
        boolean toConsole = getBoolean(overrideKey, "console-notifications", false);
        List<String> types = getStringList(overrideKey, "types");

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

    private void playNotificationSound(String overrideKey) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playNotificationSound(overrideKey, player);
        }
    }

    private void playNotificationSound(String overrideKey, Player player) {
        if (!getBoolean(overrideKey, "sound.enabled", false)) return;

        String soundName = getString(overrideKey, "sound.name", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) getDouble(overrideKey, "sound.volume", 1.0);
        float pitch = (float) getDouble(overrideKey, "sound.pitch", 1.0);

        XSound.matchXSound(soundName).ifPresent(xSound -> xSound.play(player, volume, pitch));
    }

    private String overridePath(String overrideKey, String field) {
        return "notifications." + overrideKey + "." + field;
    }

    private boolean getBoolean(String overrideKey, String field, boolean def) {
        String path = overrideKey != null ? overridePath(overrideKey, field) : null;
        if (path != null && module.getConfig().contains(path)) {
            return module.getConfig().getBoolean(path);
        }
        return module.getConfig().getBoolean("notifications." + field, def);
    }

    private String getString(String overrideKey, String field, String def) {
        String path = overrideKey != null ? overridePath(overrideKey, field) : null;
        if (path != null && module.getConfig().contains(path)) {
            return module.getConfig().getString(path, def);
        }
        return module.getConfig().getString("notifications." + field, def);
    }

    private double getDouble(String overrideKey, String field, double def) {
        String path = overrideKey != null ? overridePath(overrideKey, field) : null;
        if (path != null && module.getConfig().contains(path)) {
            return module.getConfig().getDouble(path, def);
        }
        return module.getConfig().getDouble("notifications." + field, def);
    }

    private List<String> getStringList(String overrideKey, String field) {
        String path = overrideKey != null ? overridePath(overrideKey, field) : null;
        if (path != null && module.getConfig().contains(path)) {
            return module.getConfig().getStringList(path);
        }
        return module.getConfig().getStringList("notifications." + field);
    }

    public void shutdown() {
    }
}
