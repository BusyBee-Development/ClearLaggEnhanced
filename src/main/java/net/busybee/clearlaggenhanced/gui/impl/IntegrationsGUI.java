package net.busybee.clearlaggenhanced.gui.impl;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.gui.base.InventoryGUI;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class IntegrationsGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private record IntegrationInfo(String id, String name, String material) {}

    private final List<IntegrationInfo> integrations = List.of(
            new IntegrationInfo("rosestacker", "RoseStacker", "ROSE_BUSH"),
            new IntegrationInfo("wildstacker", "WildStacker", "RECOVERY_COMPASS"),
            new IntegrationInfo("modernshowcase", "ModernShowcase", "ARMOR_STAND"),
            new IntegrationInfo("griefprevention3d", "GriefPrevention3D", "GOLDEN_SHOVEL")
    );

    public IntegrationsGUI(ClearLaggEnhanced plugin) {
        super(27, ChatColor.translateAlternateColorCodes('&', "&2&lPlugin Integrations"));
        this.plugin = plugin;
    }

    @Override
    public void decorate(Player player) {
        // Fill background
        ItemStack glass = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        if (glass != null) {
            ItemMeta meta = glass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                glass.setItemMeta(meta);
            }
            for (int i = 0; i < 27; i++) {
                setItem(i, glass);
            }
        }

        int slot = 10;
        for (IntegrationInfo info : integrations) {
            Module module = plugin.getModuleManager().getModule(info.id());
            boolean enabled = module != null && module.isEnabled();
            boolean available = module != null && module.isAvailable();

            setItem(slot, createIntegrationItem(info, enabled, available), event -> {
                if (module == null) return;

                boolean newState = !module.isEnabled();
                plugin.getModuleManager().setModuleEnabled(module, newState);

                Player clicker = (Player) event.getWhoClicked();
                XSound.BLOCK_NOTE_BLOCK_PLING.play(clicker, 1.0f, newState ? 2.0f : 0.5f);
                clicker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&a[CLE] &7Integration &e" + info.name() + " &7is now " + (newState ? "&aEnabled" : "&cDisabled")));

                decorate(player);
            });
            slot++;
            if (slot == 13) slot = 14; // Skip middle slot
        }

        // Back button
        ItemStack backItem = XMaterial.BARRIER.parseItem();
        if (backItem != null) {
            ItemMeta meta = backItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cBack to Admin Menu"));
                backItem.setItemMeta(meta);
            }
        }
        setItem(22, backItem, event -> {
            new AdminGUI(plugin, plugin.getGuiRegistry()).open((Player) event.getWhoClicked());
        });
    }

    private ItemStack createIntegrationItem(IntegrationInfo info, boolean enabled, boolean available) {
        ItemStack item = XMaterial.matchXMaterial(info.material()).map(XMaterial::parseItem).orElse(XMaterial.PAPER.parseItem());
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', (enabled ? "&a" : "&c") + info.name()));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: " + (enabled ? "&aEnabled" : "&cDisabled")));

                if (!available) {
                    lore.add("");
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&c&lWARNING: &7Dependency missing!"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7This integration will not function"));
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7until the target plugin is installed."));
                }

                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to toggle"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
