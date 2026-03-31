package com.clearlagenhanced.modules.moblimiter.inventory;

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

public class MobLimiterGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private final Module module;
    
    public MobLimiterGUI(ClearLaggEnhanced plugin, Module module) {
        this.plugin = plugin;
        this.module = module;
    }
    
    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&6&lMob Limiter"));
    }
    
    @Override
    public void decorate(Player player) {
        boolean enabled = module.isEnabled();
        int maxMobs = module.getConfig().getInt("max-mobs-per-chunk", 50);
        
        addButton(11, new InventoryButton()
            .creator(p -> createToggleItem(enabled))
            .consumer(event -> {
                module.setEnabled(!enabled);
                if (module.isEnabled()) {
                    module.onEnable();
                } else {
                    module.onDisable();
                }
                plugin.getGuiManager().openGUI(new MobLimiterGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(13, new InventoryButton()
            .creator(p -> createInfoItem(maxMobs))
            .consumer(event -> {})
        );
        
        addButton(22, new InventoryButton()
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
    
    private ItemStack createInfoItem(int maxMobs) {
        ItemStack item = XMaterial.ZOMBIE_HEAD.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eGlobal Limit: &f" + maxMobs));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Max mobs per chunk: " + maxMobs));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Per-type limits configured in config.yml"));
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