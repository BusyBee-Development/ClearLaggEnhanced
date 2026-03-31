package com.clearlagenhanced.modules.spawnerlimiter.inventory;

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

public class SpawnerLimiterGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private final Module module;
    
    public SpawnerLimiterGUI(ClearLaggEnhanced plugin, Module module) {
        this.plugin = plugin;
        this.module = module;
    }
    
    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, ChatColor.translateAlternateColorCodes('&', "&d&lSpawner Limiter"));
    }
    
    @Override
    public void decorate(Player player) {
        boolean enabled = module.isEnabled();
        double multiplier = module.getConfig().getDouble("spawn-delay-multiplier", 1.5);
        
        addButton(11, new InventoryButton()
            .creator(p -> createToggleItem(enabled))
            .consumer(event -> {
                module.setEnabled(!enabled);
                module.getConfig().set("enabled", !enabled);
                module.saveConfig();
                if (module.isEnabled()) {
                    module.onEnable();
                } else {
                    module.onDisable();
                }
                plugin.getGuiManager().openGUI(new SpawnerLimiterGUI(plugin, module), (Player) event.getWhoClicked());
            })
        );
        
        addButton(13, new InventoryButton()
            .creator(p -> createMultiplierItem(multiplier))
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                placeholders.put("type", "Spawn Delay Multiplier");
                MessageUtils.sendMessage(clicker, "gui.enter-value", placeholders);
                clicker.closeInventory();

                plugin.getChatInputManager().requestInput(clicker, input -> {
                    if (input != null) {
                        try {
                            double newVal = Double.parseDouble(input);
                            if (newVal < 0) {
                                MessageUtils.sendMessage(clicker, "gui.invalid-double");
                            } else {
                                module.getConfig().set("spawn-delay-multiplier", newVal);
                                module.saveConfig();
                                module.onReload();
                                java.util.Map<String, String> successPlaceholders = new java.util.HashMap<>();
                                successPlaceholders.put("type", "Spawn Delay Multiplier");
                                successPlaceholders.put("value", input);
                                MessageUtils.sendMessage(clicker, "gui.value-set", successPlaceholders);
                            }
                        } catch (NumberFormatException e) {
                            MessageUtils.sendMessage(clicker, "gui.invalid-double");
                        }
                    }
                    plugin.getGuiManager().openGUI(new SpawnerLimiterGUI(plugin, module), clicker);
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
    
    private ItemStack createMultiplierItem(double multiplier) {
        ItemStack item = XMaterial.SPAWNER.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eDelay Multiplier: &f" + multiplier + "x"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Spawner delays multiplied by: " + multiplier));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Edit in config.yml"));
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
