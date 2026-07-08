package net.busybee.clearlaggenhanced.libs.fastinv;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manager for FastInv listeners.
 *
 * @author MrMicky
 */
public final class FastInvManager {

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private FastInvManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * Register listeners for FastInv.
     *
     * @param plugin plugin to register
     * @throws NullPointerException if plugin is null
     * @throws IllegalStateException if FastInv is already registered
     */
    public static void register(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (REGISTERED.getAndSet(true)) {
            throw new IllegalStateException("FastInv is already registered");
        }

        Bukkit.getPluginManager().registerEvents(new InventoryListener(plugin), plugin);
    }

    /**
     * Close all open FastInv inventories.
     */
    public static void closeAll() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            try {
                Inventory topInventory = p.getOpenInventory().getTopInventory();
                if (topInventory.getHolder() instanceof FastInv) {
                    p.closeInventory();
                }
            } catch (Exception ignored) {
                // If getHolder() fails (e.g. during shutdown), we safely ignore it.
            }
        });
    }

    public static final class InventoryListener implements Listener {

        private final Plugin plugin;

        public InventoryListener(Plugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            try {
                if (e.getInventory().getHolder() instanceof FastInv && e.getClickedInventory() != null) {
                    FastInv inv = (FastInv) e.getInventory().getHolder();

                    boolean wasCancelled = e.isCancelled();
                    e.setCancelled(true);

                    inv.handleClick(e);

                    // This prevents un-canceling the event if another plugin canceled it before
                    if (!wasCancelled && !e.isCancelled()) {
                        e.setCancelled(false);
                    }
                }
            } catch (Exception ignored) {}
        }

        @EventHandler
        public void onInventoryOpen(InventoryOpenEvent e) {
            try {
                if (e.getInventory().getHolder() instanceof FastInv) {
                    FastInv inv = (FastInv) e.getInventory().getHolder();

                    inv.handleOpen(e);
                }
            } catch (Exception ignored) {}
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent e) {
            try {
                if (e.getInventory().getHolder() instanceof FastInv) {
                    FastInv inv = (FastInv) e.getInventory().getHolder();

                    if (this.plugin.isEnabled() && inv.handleClose(e)) {
                        Bukkit.getScheduler().runTask(this.plugin, () -> inv.open((Player) e.getPlayer()));
                    }
                }
            } catch (Exception ignored) {}
        }

        @EventHandler
        public void onPluginDisable(PluginDisableEvent e) {
            if (e.getPlugin() == this.plugin) {
                closeAll();

                REGISTERED.set(false);
            }
        }
    }
}
