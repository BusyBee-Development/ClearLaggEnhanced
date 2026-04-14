package com.clearlagenhanced.modules.miscentitylimiter.inventory;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.inventory.InventoryGUI;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MiscEntityLimiterGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private final Module module;
    
    public MiscEntityLimiterGUI(ClearLaggEnhanced plugin, Module module) {
        super(27, ChatColor.translateAlternateColorCodes('&', "&b&lMisc Entity Limiter"));
        this.plugin = plugin;
        this.module = module;
    }
    
    @Override
    public void decorate(Player player) {
        boolean enabled = module.isEnabled();
        
        setItem(13, createToggleItem(enabled), event -> {
            plugin.getModuleManager().setModuleEnabled(module, !enabled);
            new MiscEntityLimiterGUI(plugin, module).open((Player) event.getWhoClicked());
        });
        
        setItem(22, createBackItem(), event -> {
            Player clicker = (Player) event.getWhoClicked();
            clicker.closeInventory();
            clicker.performCommand("lagg admin");
        });
    }
    
    private ItemStack createToggleItem(boolean enabled) {
        ItemStack item = enabled ? XMaterial.GREEN_WOOL.parseItem() : XMaterial.RED_WOOL.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', enabled ? "&aEnabled" : "&cDisabled"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Limits armor stands, item frames"));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to " + (enabled ? "disable" : "enable")));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
    
    private ItemStack createBackItem() {
        ItemStack item = XMaterial.ARROW.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fBack to Main Menu"));
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
