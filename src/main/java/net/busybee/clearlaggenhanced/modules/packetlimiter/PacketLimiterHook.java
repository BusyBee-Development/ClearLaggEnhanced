package net.busybee.clearlaggenhanced.modules.packetlimiter;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PacketLimiterHook {
    private final PacketLimiterModule module;
    private final ClearLaggEnhanced plugin;
    private ProtocolManager protocolManager;

    public PacketLimiterHook(PacketLimiterModule module, ClearLaggEnhanced plugin) {
        this.module = module;
        this.plugin = plugin;
    }

    public void register(List<String> packetNames) {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        
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
            module.setEnabled(false);
            return;
        }

        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, types) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) return;

                if (module.shouldCancelPacket(player)) {
                    event.setCancelled(true);
                } else {
                    module.processPacket(player);
                }
            }
        });
    }

    public void unregister() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(plugin);
            protocolManager = null;
        }
    }
}
