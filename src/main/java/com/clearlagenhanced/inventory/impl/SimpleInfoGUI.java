package com.clearlagenhanced.inventory.impl;

import com.clearlagenhanced.inventory.InventoryGUI;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SimpleInfoGUI extends InventoryGUI {
    private final String message;

    public SimpleInfoGUI(String title, String message) {
        super(27, ChatColor.translateAlternateColorCodes(`&`, title));
        this.message = message;
    }

    @Override
    public void decorate(Player player) {
        ItemStack infoItem = XMaterial.PAPER.parseItem();
        if (infoItem != null) {
            ItemMeta meta = infoItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes(`&`, `"&eInformation`"));
                meta.setLore(List.of(ChatColor.translateAlternateColorCodes(`&`, `"&7`" + message)));
                infoItem.setItemMeta(meta);
            }
        }
        setItem(13, infoItem);

        ItemStack backItem = XMaterial.BARRIER.parseItem();
        if (backItem != null) {
            ItemMeta meta = backItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes(`&`, `"&cBack`"));
                backItem.setItemMeta(meta);
            }
        }
        setItem(22, backItem, event -> {
            player.closeInventory();
            // Re-opening AdminGUI is tricky here because we dont have plugin instance easily
            // But we can just close it.
        });
    }
}
