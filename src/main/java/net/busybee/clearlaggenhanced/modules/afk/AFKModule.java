package net.busybee.clearlaggenhanced.modules.afk;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.utils.MessageUtils;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AFKModule extends Module implements Listener {
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> originalDistance = new ConcurrentHashMap<>();
    private WrappedTask task;

    public AFKModule(ClearLaggEnhanced plugin) {
        super("AFK Optimization", "afk");
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startTask();
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        stopTask();
        for (UUID uuid : originalDistance.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setSimulationDistance(originalDistance.get(uuid));
            }
        }
        lastActivity.clear();
        originalDistance.clear();
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }

    private void startTask() {
        task = ClearLaggEnhanced.scheduler().runTimer(() -> {
            long now = System.currentTimeMillis();
            long threshold = getInt("afk-threshold", 300) * 1000L;
            int afkDistance = getInt("afk-simulation-distance", 2);

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                long last = lastActivity.getOrDefault(uuid, now);

                if (now - last > threshold) {
                    if (!originalDistance.containsKey(uuid)) {
                        int current = player.getSimulationDistance();
                        if (current > afkDistance) {
                            originalDistance.put(uuid, current);
                            ClearLaggEnhanced.scheduler().runAtEntity(player, t -> {
                                player.setSimulationDistance(afkDistance);
                                if (getBoolean("notify-player", false)) {
                                    MessageUtils.sendMessage(player, "notifications.afk.distance-lowered");
                                }
                            });
                        }
                    }
                } else {
                    if (originalDistance.containsKey(uuid)) {
                        int original = originalDistance.remove(uuid);
                        ClearLaggEnhanced.scheduler().runAtEntity(player, t -> {
                            player.setSimulationDistance(original);
                            if (getBoolean("notify-player", false)) {
                                MessageUtils.sendMessage(player, "notifications.afk.distance-restored");
                            }
                        });
                    }
                }
            }
        }, 100L, 100L); // Every 5 seconds
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void updateActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockY() != event.getTo().getBlockY() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            updateActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastActivity.remove(event.getPlayer().getUniqueId());
        originalDistance.remove(event.getPlayer().getUniqueId());
    }
}