package com.clearlagenhanced.modules.entityclearing.inventory;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.inventory.InventoryButton;
import com.clearlagenhanced.inventory.InventoryGUI;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EntityClearingGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private final Module module;
    
    public EntityClearingGUI(ClearLaggEnhanced plugin, Module module) {
        this.plugin = plugin;
        this.module = module;
    }
    
    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 36, ChatColor.translateAlternateColorCodes('&', "&c&lEntity Clearing"));
    }
    
    @Override
    public void decorate(Player player) {
        boolean enabled = module.isEnabled();
        int interval = module.getConfig().getInt("interval", 300);
        boolean protectNamed = module.getConfig().getBoolean("protect-named-entities", true);
        boolean protectTamed = module.getConfig().getBoolean("protect-tamed-entities", true);
        boolean protectStacked = module.getConfig().getBoolean("protect-stacked-entities", true);
        
        addButton(10, new InventoryButton()
            .creator(p -> createToggleItem(enabled))
            .consumer(event -> {
                module.setEnabled(!enabled);
                if (module.isEnabled()) {
                    module.onEnable();
                } else {
                    module.onDisable();
                }
                plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(12, new InventoryButton()
            .creator(p -> createIntervalItem(interval))
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                clicker.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eType new interval in chat (or 'cancel'):"));
                clicker.closeInventory();
            })
        );
        
        addButton(14, new InventoryButton()
            .creator(p -> createProtectionItem("Named Entities", protectNamed))
            .consumer(event -> {
                module.getConfig().set("protect-named-entities", !protectNamed);
                plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(16, new InventoryButton()
            .creator(p -> createProtectionItem("Tamed Entities", protectTamed))
            .consumer(event -> {
                module.getConfig().set("protect-tamed-entities", !protectTamed);
                plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(20, new InventoryButton()
            .creator(p -> createProtectionItem("Stacked Entities", protectStacked))
            .consumer(event -> {
                module.getConfig().set("protect-stacked-entities", !protectStacked);
                plugin.getGuiManager().openGUI(new EntityClearingGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(31, new InventoryButton()
            .creator(p -> createBackItem())
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                clicker.closeInventory();
                clicker.performCommand("lagg admin");
            })
        );
        
        super.decorate(player);
    }
    
    private ItemStack createToggleItem(boolean enabled) {
        ItemStack item = enabled ? XMaterial.GREEN_WOOL.parseItem() : XMaterial.RED_WOOL.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', enabled ? "&aEnabled" : "&cDisabled"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to " + (enabled ? "disable" : "enable")));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
    
    private ItemStack createIntervalItem(int interval) {
        ItemStack item = XMaterial.CLOCK.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eInterval: &f" + interval + "s"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Current: " + interval + " seconds"));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&aClick to change"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
    
    private ItemStack createProtectionItem(String name, boolean enabled) {
        ItemStack item = enabled ? XMaterial.GREEN_WOOL.parseItem() : XMaterial.RED_WOOL.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eProtect " + name + ": " + (enabled ? "&aYes" : "&cNo")));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to toggle"));
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