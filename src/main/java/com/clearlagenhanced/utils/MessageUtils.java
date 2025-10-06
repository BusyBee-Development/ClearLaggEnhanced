package com.clearlagenhanced.utils;

import com.clearlagenhanced.managers.MessageManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MessageUtils {
    
    private static MessageManager messageManager;
    
    public static void initialize(MessageManager manager) {
        messageManager = manager;
    }

    public static void sendMessage(Player player, String path) {
        sendMessage(player, path, new HashMap<>());
    }
    
    public static void sendMessage(Player player, String path, Map<String, String> placeholders) {
        if (messageManager == null) {
            player.sendMessage(Component.text("MessageManager not initialized!"));
            return;
        }
        
        Component message = messageManager.getMessage(path, placeholders, player);
        player.sendMessage(message);
    }
    
    public static void sendMessage(Player player, String path, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        sendMessage(player, path, placeholders);
    }

    public static void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(message);
    }
    
    public static void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, new HashMap<>());
    }
    
    public static void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        if (messageManager == null) {
            sender.sendMessage(Component.text("MessageManager not initialized!"));
            return;
        }
        
        Component message;
        if (sender instanceof Player) {
            message = messageManager.getMessage(path, placeholders, (Player) sender);
        } else {
            message = messageManager.getMessage(path, placeholders);
        }

        sender.sendMessage(message);
    }
    
    public static void sendMessage(CommandSender sender, String path, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        sendMessage(sender, path, placeholders);
    }

    public static Component getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }
    
    public static Component getMessage(String path, Map<String, String> placeholders) {
        if (messageManager == null) {
            return Component.text("MessageManager not initialized!");
        }
        
        return messageManager.getMessage(path, placeholders);
    }
}
