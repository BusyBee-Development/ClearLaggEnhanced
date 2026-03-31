package com.clearlagenhanced.inventory.impl;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.gui.ModuleGUIRegistry;
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
import java.util.Map;

public class AdminGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private final ModuleGUIRegistry guiRegistry;
    
    public AdminGUI(ClearLaggEnhanced plugin, ModuleGUIRegistry guiRegistry) {
        this.plugin = plugin;
        this.guiRegistry = guiRegistry;
    }
    
    @Override
    protected Inventory createInventory() {
        int size = calculateInventorySize();
        return Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', "&2&lClearLagg Enhanced"));
    }
    
    private int calculateInventorySize() {
        int items = guiRegistry.getRegisteredGUIs().size();
        // Each row holds 7 modules (skipping the first and last columns)
        int rowsNeeded = (int) Math.ceil(items / 7.0);
        // We need 1 top padding row, the module rows, and 1 bottom padding row
        int totalRows = Math.max(3, rowsNeeded + 2); 
        return Math.min(54, totalRows * 9);
    }
    
    @Override
    public void decorate(Player player) {
        List<String> sortedKeys = new ArrayList<>(guiRegistry.getRegisteredGUIs().keySet());
        sortedKeys.sort(String::compareTo);
        
        int slot = 10;
        int maxSlot = getInventory().getSize() - 10; // Stay within bounds and leave room for reload button
        
        for (String moduleId : sortedKeys) {
            if (slot > maxSlot) break; // Safety check
            
            ModuleGUIRegistry.ModuleGUIInfo info = guiRegistry.getGUIInfo(moduleId);
            
            addButton(slot, new InventoryButton()
                .creator(p -> createModuleItem(info.displayName(), info.iconMaterial()))
                .consumer(event -> {
                    Player clicker = (Player) event.getWhoClicked();
                    InventoryGUI moduleGUI = info.guiSupplier().get();
                    plugin.getGuiManager().openGUI(moduleGUI, clicker);
                })
            );
            
            slot++;
            if (slot % 9 == 8) { // Skip edge columns
                slot += 2;
            }
        }
        
        ItemStack reloadItem = XMaterial.COMMAND_BLOCK.parseItem();
        if (reloadItem != null) {
            ItemMeta meta = reloadItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bReload Config"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Reload all plugin configurations"));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&aClick to reload"));
                meta.setLore(lore);
                reloadItem.setItemMeta(meta);
            }
        }
        
        addButton(getInventory().getSize() - 5, new InventoryButton()
            .creator(p -> reloadItem)
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                clicker.closeInventory();
                plugin.reloadAll(clicker);
            })
        );
        
        super.decorate(player);
    }
    
    private ItemStack createModuleItem(String name, String materialName) {
        ItemStack item = XMaterial.matchXMaterial(materialName).map(XMaterial::parseItem).orElse(XMaterial.PAPER.parseItem());
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e" + name));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to configure"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}