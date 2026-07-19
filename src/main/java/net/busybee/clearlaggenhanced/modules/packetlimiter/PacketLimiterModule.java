package net.busybee.clearlaggenhanced.modules.packetlimiter;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PacketLimiterModule extends Module {
    private ProtocolManager protocolManager;
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

        registerListeners();
    }

    @Override
    public void onDisable() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(plugin);
            protocolManager = null;
        }
        playerData.clear();
        blockedPlayers.clear();
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }

    private void registerListeners() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        
        List<String> packetNames = getStringList("monitored-packets");
        List<PacketType> types = new ArrayList<>();
        
        for (String name : packetNames) {
            try {
                java.lang.reflect.Field field = PacketType.Play.Client.class.getField(name);
                PacketType type = (PacketType) field.get(null);
                types.add(type);
            } catch (Exception ignored) {}
        }

        if (types.isEmpty()) {
            plugin.getLogger().warning("No valid packet types found for Packet Limiter! Disabling module.");
            setEnabled(false);
            return;
        }

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, types) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handlePacket(event);
            }
        });
    }

    private void handlePacket(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        if (player.hasPermission("clearlag.packetlimit.bypass")) return;

        UUID uuid = player.getUniqueId();
        
        // Check if player is currently blocked
        Long unblockTime = blockedPlayers.get(uuid);
        if (unblockTime != null) {
            if (System.currentTimeMillis() < unblockTime) {
                event.setCancelled(true);
                return;
            } else {
                blockedPlayers.remove(uuid);
            }
        }

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