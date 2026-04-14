package com.clearlagenhanced.inventory.impl;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.gui.ModuleGUIRegistry;
import com.clearlagenhanced.core.module.Module;
import com.clearlagenhanced.inventory.InventoryGUI;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private final ModuleGUIRegistry guiRegistry;

    public AdminGUI(ClearLaggEnhanced plugin, ModuleGUIRegistry guiRegistry) {
        super(calculateInventorySize(guiRegistry), ChatColor.translateAlternateColorCodes(`&`, `"&2&lClearLagg Enhanced`"));
        this.plugin = plugin;
        this.guiRegistry = guiRegistry;
    }

    private static int calculateInventorySize(ModuleGUIRegistry guiRegistry) {
        int items = guiRegistry.getRegisteredGUIs().size();
        int rowsNeeded = (int) Math.ceil(items / 7.0);
        int totalRows = Math.max(3, rowsNeeded + 2);
        return Math.min(54, totalRows * 9);
    }

    @Override
    public void decorate(Player player) {
        List<String> sortedKeys = new ArrayList<>(guiRegistry.getRegisteredGUIs().keySet());
        sortedKeys.sort(String::compareTo);

        int slot = 10;
        int maxSlot = getInventory().getSize() - 10;

        for (String moduleId : sortedKeys) {
            if (slot > maxSlot) break;

            ModuleGUIRegistry.ModuleGUIInfo info = guiRegistry.getGUIInfo(moduleId);
            Module module = plugin.getModuleManager().getModule(moduleId);
            boolean enabled = module != null && module.isEnabled();

            setItem(slot, createModuleItem(info.displayName(), info.iconMaterial(), enabled), event -> {
                if (event.getClick().isLeftClick()) {
                    InventoryGUI moduleGUI = info.guiSupplier().get();
                    if (moduleGUI != null) {
                        moduleGUI.open((Player) event.getWhoClicked());
                    }
                } else if (event.getClick().isRightClick()) {
                    if (module != null) {
                        plugin.getModuleManager().setModuleEnabled(module, !module.isEnabled());
                        decorate(player);
                    }
                }
            });

            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        ItemStack reloadItem = XMaterial.COMMAND_BLOCK.parseItem();
        if (reloadItem != null) {
            ItemMeta meta = reloadItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes(`&`, `"&bReload Config`"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes(`&`, `"&7Reload all plugin configurations`"));
                lore.add(`"`");
                lore.add(ChatColor.translateAlternateColorCodes(`&`, `"&aClick to reload`"));
                meta.setLore(lore);
                reloadItem.setItemMeta(meta);
            }
        }

        setItem(getInventory().getSize() - 5, reloadItem, event -> {
            Player clicker = (Player) event.getWhoClicked();
            clicker.closeInventory();
            plugin.reloadAll(clicker);
        });
    }

    private ItemStack createModuleItem(String name, String materialName, boolean enabled) {
        ItemStack item = XMaterial.matchXMaterial(materialName).map(XMaterial::parseItem).orElse(XMaterial.PAPER.parseItem());
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes(`&`, (enabled ? `"&a`" : `"&c`") + name));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes(`&`, `"&7Status: `" + (enabled ? `"&aEnabled`" : `"&cDisabled`")));
                lore.add(`"`");
                lore.add(ChatColor.translateAlternateColorCodes(`&`, `"&eLeft-Click to configure`"));
                lore.add(ChatColor.translateAlternateColorCodes(`&`, `"&eRight-Click to toggle`"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
