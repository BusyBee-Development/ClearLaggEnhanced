package com.clearlagenhanced.modules.moblimiter.inventory;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.inventory.InventoryButton;
import com.clearlagenhanced.inventory.InventoryGUI;
import com.clearlagenhanced.utils.MessageUtils;
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
                plugin.getModuleManager().setModuleEnabled(module, !enabled);
                plugin.getGuiManager().openGUI(new MobLimiterGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(13, new InventoryButton()
            .creator(p -> createInfoItem(maxMobs))
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                placeholders.put("type", "Global Mob Limit");
                MessageUtils.sendMessage(clicker, "gui.enter-value", placeholders);
                clicker.closeInventory();

                plugin.getChatInputManager().requestInput(clicker, input -> {
                    if (input != null) {
                        try {
                            int newVal = Integer.parseInt(input);
                            if (newVal < 1) {
                                MessageUtils.sendMessage(clicker, "gui.invalid-number");
                            } else {
                                module.getConfig().set("max-mobs-per-chunk", newVal);
                                module.saveConfig();
                                module.onReload();
                                java.util.Map<String, String> successPlaceholders = new java.util.HashMap<>();
                                successPlaceholders.put("type", "Global Mob Limit");
                                successPlaceholders.put("value", input);
                                MessageUtils.sendMessage(clicker, "gui.value-set", successPlaceholders);
                            }
                        } catch (NumberFormatException e) {
                            MessageUtils.sendMessage(clicker, "gui.invalid-number");
                        }
                    }
                    plugin.getGuiManager().openGUI(new MobLimiterGUI(plugin, module), clicker);
                });
            })
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
