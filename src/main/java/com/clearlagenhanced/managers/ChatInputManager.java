package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {
    private final ClearLaggEnhanced plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputManager(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
    }

    public void requestInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
        MessageUtils.sendMessage(player, "gui.type-cancel");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pendingInputs.remove(player.getUniqueId());

        if (callback != null) {
            event.setCancelled(true);
            String message = event.getMessage();

            if (message.equalsIgnoreCase("cancel")) {
                MessageUtils.sendMessage(player, "gui.input-cancelled");
                ClearLaggEnhanced.scheduler().runNextTick(task -> callback.accept(null));
                return;
            }

            ClearLaggEnhanced.scheduler().runNextTick(task -> callback.accept(message));
        }
    }
}
