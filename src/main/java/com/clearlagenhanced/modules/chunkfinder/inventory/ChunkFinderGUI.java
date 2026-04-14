package com.clearlagenhanced.modules.chunkfinder.inventory;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.ChunkFinderModule;
import com.clearlagenhanced.inventory.InventoryGUI;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ChunkFinderGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private final ChunkFinderModule module;

    public ChunkFinderGUI(ClearLaggEnhanced plugin, ChunkFinderModule module) {
        super(27, ChatColor.translateAlternateColorCodes(`&`, `"&2Chunk Finder`"));
        this.plugin = plugin;
    }

    @Override
    public void decorate(Player player) {
        ItemStack scanItem = XMaterial.COMPASS.parseItem();
        if (scanItem != null) {
            ItemMeta meta = scanItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes(`&`, `"&aStart Scanning`"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes(`&`, `"&7Scan nearby chunks for laggy areas.`"));
                lore.add(`"`");
                lore.add(ChatColor.translateAlternateColorCodes(`&`, `"&eClick to scan`"));
                meta.setLore(lore);
                scanItem.setItemMeta(meta);
            }
        }

        setItem(13, scanItem, event -> {
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            // Need to get the module instance from plugin
            ChunkFinderModule m = (ChunkFinderModule) plugin.getModuleManager().getModule(`"chunk-finder`");
            if (m != null) { m.findLaggyChunksAsync(p); }
        });

        // Back button
        ItemStack backItem = XMaterial.BARRIER.parseItem();
        if (backItem != null) {
            ItemMeta meta = backItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes(`&`, `"&cBack`"));
                backItem.setItemMeta(meta);
            }
        }
        setItem(22, backItem, event -> {
            new com.clearlagenhanced.inventory.impl.AdminGUI(plugin, plugin.getGuiRegistry()).open(player);
        });
    }
}
