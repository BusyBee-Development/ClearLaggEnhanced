package net.busybee.clearlaggenhanced.modules.packetlimiter;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PacketLimiterModule extends Module {
    private PacketLimiterHook hook;
    private final Map<UUID, PlayerPacketData> playerData = new ConcurrentHashMap<>();
    private final Map<UUID, Long> blockedPlayers = new ConcurrentHashMap<>();

    public PacketLimiterModule(ClearLaggEnhanced plugin) {
        super("Packet Limiter", "packet-limiter");
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
    }

    @Override
    public void onEnable() {
        if (!isAvailable()) {
            plugin.getLogger().warning("ProtocolLib not found! Packet Limiter module will be disabled.");
            setEnabled(false);
            return;
        }

        this.hook = new PacketLimiterHook(this, plugin);
        this.hook.register(getStringList("monitored-packets"));
    }

    @Override
    public void onDisable() {
        if (hook != null) {
            hook.unregister();
            hook = null;
        }
        playerData.clear();
        blockedPlayers.clear();
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }

    public boolean shouldCancelPacket(Player player) {
        if (player.hasPermission("clearlag.packetlimit.bypass")) return false;

        UUID uuid = player.getUniqueId();
        
        // Check if player is currently blocked
        Long unblockTime = blockedPlayers.get(uuid);
        if (unblockTime != null) {
            if (System.currentTimeMillis() < unblockTime) {
                return true;
            } else {
                blockedPlayers.remove(uuid);
            }
        }
        return false;
    }

    public void processPacket(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerPacketData data = playerData.computeIfAbsent(uuid, k -> new PlayerPacketData());
        long now = System.currentTimeMillis();
        
        data.packetCount++;
        
        if (now - data.lastReset > 1000L) {
            checkLimit(player, data);
            data.packetCount = 0;
            data.lastReset = now;
        }
    }

    private void checkLimit(Player player, PlayerPacketData data) {
        int maxPPS = getInt("max-packets-per-second", 150);
        
        if (data.packetCount > maxPPS) {
            data.violations++;
            
            if (getBoolean("notify-admins", true)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());
                placeholders.put("pps", String.valueOf(data.packetCount));
                placeholders.put("violations", String.valueOf(data.violations));
                
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("clearlag.admin"))
                        .forEach(p -> MessageUtils.sendMessage(p, "notifications.packet-limiter.admin-notify", placeholders));
            }

            if (getBoolean("notify-player", true)) {
                MessageUtils.sendMessage(player, "notifications.packet-limiter.player-notify");
            }

            // Temporary block
            if (data.violations >= 3) {
                int blockDuration = getInt("block-duration", 5);
                blockedPlayers.put(player.getUniqueId(), System.currentTimeMillis() + (blockDuration * 1000L));
                plugin.getLogger().warning("Blocked " + player.getName() + " for " + blockDuration + "s due to packet spam.");
            }

            // Kick
            int kickThreshold = getInt("kick-threshold", 10);
            if (data.violations >= kickThreshold) {
                ClearLaggEnhanced.scheduler().runAtEntity(player, t -> {
                    player.kickPlayer(MessageUtils.getLegacyMessage("notifications.packet-limiter.kick-message"));
                });
                playerData.remove(player.getUniqueId());
                blockedPlayers.remove(player.getUniqueId());
            }
        } else {
            // Gradually reduce violations if they are behaving
            if (data.violations > 0 && Math.random() < 0.1) {
                data.violations--;
            }
        }
    }

    private static class PlayerPacketData {
        int packetCount = 0;
        int violations = 0;
        long lastReset = System.currentTimeMillis();
    }
}