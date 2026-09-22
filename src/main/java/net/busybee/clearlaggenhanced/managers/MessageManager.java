package net.busybee.clearlaggenhanced.managers;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.updater.ConfigMigrator;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {

    private final ClearLaggEnhanced plugin;
    private FileConfiguration messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern AMP_HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_X_HEX = Pattern.compile("(?i)&x(?:&([0-9A-F])){6}");
    // Match existing MiniMessage tags first so their arguments (e.g. <gradient:#hex:#hex>) are left untouched
    private static final Pattern TAG_OR_BARE_HEX = Pattern.compile("<[^<>]*>|#([A-Fa-f0-9]{6})");
    private static final Pattern TAG_OR_LEGACY_CODE = Pattern.compile("(?i)<[^<>]*>|&([0-9a-fk-or])");

    private final boolean placeholderAPIEnabled;
    
    public MessageManager(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.placeholderAPIEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        loadMessages();
    }

    private void loadMessages() {
        try {
            ConfigMigrator migrator = new ConfigMigrator(plugin);
            messages = migrator.migrate("messages.yml");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to migrate messages.yml: " + e.getMessage());
            e.printStackTrace();
            messages = null;
        }

        if (messages == null) {
            File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
            if (!messagesFile.exists()) {
                plugin.saveResource("messages.yml", false);
            }
            messages = YamlConfiguration.loadConfiguration(messagesFile);
        }
    }

    public FileConfiguration getConfig() {
        return messages;
    }

    public String getRawMessage(@NotNull String path) {
        if (messages != null && messages.contains(path)) {
            return messages.getString(path);
        }

        return "Message not found: " + path;
    }

    public Component getMessage(@NotNull String path, @NotNull Map<String, String> placeholders) {
        String message = getRawMessage(path);
        return parseMessage(message, placeholders, null);
    }

    public Component getMessage(@NotNull String path, @NotNull Map<String, String> placeholders, @NotNull Player player) {
        String message = getRawMessage(path);
        return parseMessage(message, placeholders, player);
    }

    public String getLegacyMessage(@NotNull String path, @NotNull Map<String, String> placeholders) {
        Component component = getMessage(path, placeholders);
        return legacySerializer.serialize(component);
    }

    public Component parseMessage(@NotNull String message, Map<String, String> placeholders, Player player) {
        if (message.isEmpty()) {
            return Component.empty();
        }

        message = message.replace("{prefix}", messages.getString("prefix", ""));

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        if (placeholderAPIEnabled) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        String normalized = normalizeToMiniMessage(message);

        try {
            return miniMessage.deserialize(normalized);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse MiniMessage. Falling back to legacy. Message: " + message);
        }

        return legacySerializer.deserialize(message);
    }

    private String normalizeToMiniMessage(@NotNull String input) {
        if (input.isEmpty()) {
            return "";
        }

        String msg = input.replace('§', '&');
        msg = convertLegacyXHex(msg);
        msg = AMP_HEX.matcher(msg).replaceAll(mr -> "<#" + mr.group(1) + ">");
        msg = TAG_OR_BARE_HEX.matcher(msg).replaceAll(mr -> mr.group(1) == null
                ? Matcher.quoteReplacement(mr.group())
                : "<#" + mr.group(1) + ">");
        msg = convertLegacyCodesToMini(msg);

        return msg;
    }

    private String convertLegacyXHex(@NotNull String msg) {
        Matcher m = LEGACY_X_HEX.matcher(msg);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String full = m.group(); // e.g., &x&F&F&0&0&0&0
            StringBuilder hex = new StringBuilder(6);
            for (int i = 3; i < full.length(); i += 2) {
                char ch = full.charAt(i);
                if (isHex(ch)) hex.append(ch);
            }

            if (hex.length() == 6) {
                m.appendReplacement(sb, '<' + "#" + hex + '>');
            } else {
                m.appendReplacement(sb, full);
            }
        }

        m.appendTail(sb);
        return sb.toString();
    }

    private boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    private String convertLegacyCodesToMini(@NotNull String msg) {
        return TAG_OR_LEGACY_CODE.matcher(msg).replaceAll(mr -> mr.group(1) == null
                ? Matcher.quoteReplacement(mr.group())
                : "<" + mapLegacyToMiniTag(Character.toLowerCase(mr.group(1).charAt(0))) + ">");
    }

    private String mapLegacyToMiniTag(char code) {
        return switch (code) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            case 'r' -> "reset";
            default -> null;
        };
    }
}
