package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationManager {

    private final ClearLaggEnhanced plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final PlatformScheduler scheduler;
    private final List<WrappedTask> warningTasks = new ArrayList<>();

    public NotificationManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.scheduler = ClearLaggEnhanced.scheduler();
    }

    public void sendClearWarnings() {
        cancelWarnings();

        List<Integer> broadcastTimes = configManager.getIntegerList("notifications.broadcast-times");

        if (broadcastTimes.isEmpty()) {
            warningTasks.add(scheduler.runLater(this::performClear, 1L));
            return;
        }

        int maxTime = broadcastTimes.stream().max(Integer::compareTo).orElse(0);

        for (int seconds : broadcastTimes) {
            long delayTicks = (maxTime - seconds) * 20L;
            if (delayTicks <= 0L) {
                sendWarning(seconds);
            } else {
                warningTasks.add(scheduler.runLater(() -> sendWarning(seconds), delayTicks));
            }
        }

        long clearDelayTicks = maxTime * 20L;
        if (clearDelayTicks <= 0L) {
            performClear();
        } else {
            warningTasks.add(scheduler.runLater(this::performClear, clearDelayTicks));
        }
    }

    private void sendWarning(int seconds) {
        List<String> notificationTypes = configManager.getStringList("notifications.types");
        if (notificationTypes.isEmpty()) {
            notificationTypes.add("ACTION_BAR");
        }

        boolean soundEnabled = configManager.getBoolean("notifications.sound.enabled", true);
        String soundName = configManager.getString("notifications.sound.name", "BLOCK_NOTE_BLOCK_PLING");
        float volume = (float) configManager.getDouble("notifications.sound.volume", 1.0);
        float pitch = (float) configManager.getDouble("notifications.sound.pitch", 1.2);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("seconds", String.valueOf(seconds));

        Component message = messageManager.getMessage("warnings.entity-clear", placeholders);

        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String notificationType : notificationTypes) {
                switch (notificationType.toUpperCase()) {
                    case "CHAT" -> player.sendMessage(message);
                    case "ACTION_BAR" -> player.sendActionBar(message);
                    case "TITLE" -> {
                        Component titleMain = messageManager.getMessage("warnings.title", java.util.Collections.emptyMap(), player);
                        Title title = Title.title(
                                titleMain,
                                message,
                                Title.Times.times(
                                        Duration.ofMillis(500), // fade in
                                        Duration.ofSeconds(2),  // stay
                                        Duration.ofMillis(500)  // fade out
                                )
                        );
                        player.showTitle(title);
                    }
                    default -> plugin.getLogger().warning("Invalid notification type: " + notificationType);
                }
            }

            if (soundEnabled) {
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound name: " + soundName + ". Skipping sound playback.");
                }
            }
        }
    }

    private void performClear() {
        scheduler.runAsync(task -> {
            long startTime = System.currentTimeMillis();
            int cleared = plugin.getEntityManager().clearEntities();
            long duration = System.currentTimeMillis() - startTime;

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("count", String.valueOf(cleared));
            placeholders.put("time", String.valueOf(duration));

            Component message = messageManager.getMessage("notifications.clear-complete", placeholders);

            if (cleared > 0) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(message);
                }
            }
        });
    }

    public void cancelWarnings() {
        if (!warningTasks.isEmpty()) {
            for (WrappedTask t : warningTasks) {
                if (t != null) {
                    t.cancel();
                }
            }
            warningTasks.clear();
        }
    }

    public void shutdown() {
        cancelWarnings();
    }
}
