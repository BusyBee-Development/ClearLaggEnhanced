package com.clearlagenhanced.modules.performance.inventory;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.PerformanceModule;
import com.clearlagenhanced.inventory.InventoryGUI;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PerformanceGUI extends InventoryGUI {
    private final ClearLaggEnhanced plugin;
    private final PerformanceModule module;

    public PerformanceGUI(ClearLaggEnhanced plugin, PerformanceModule module) {
        super(27, ChatColor.translateAlternateColorCodes(`&`, `"&2Performance Status`"));
        this.plugin = plugin;
        this.module = module;
    }

    @Override
    public void decorate(Player player) {
        // TPS Item
        double tps = module.getTPS();
        String tpsColor = tps >= 18 ? `"&a`" : (tps >= 15 ? `"&e`" : `"&c`");
        setItem(10, createInfoItem(XMaterial.CLOCK, `"&6Server TPS`", List.of(ChatColor.translateAlternateColorCodes(`&`, tpsColor + String.format(`"%.2f`", tps)))));

        // RAM Item
        setItem(13, createInfoItem(XMaterial.COMPARATOR, `"&6Memory Usage`", List.of(ChatColor.translateAlternateColorCodes(`&`, `"&7`" + module.getFormattedMemoryUsage()))));

        // Entities Item
        setItem(16, createInfoItem(XMaterial.CHICKEN_SPAWN_EGG, `"&6Total Entities`", List.of(ChatColor.translateAlternateColorCodes(`&`, `"&7`" + module.getTotalEntities()))));

        // Back button
        setItem(22, createInfoItem(XMaterial.BARRIER, `"&cBack`", List.of()), event -> {
            plugin.getGuiRegistry().getGUIInfo(`"admin`"); // This is not how it works, but I can just open AdminGUI
            new com.clearlagenhanced.inventory.impl.AdminGUI(plugin, plugin.getGuiRegistry()).open(player);
        });
    }

    private ItemStack createInfoItem(XMaterial material, String name, List<String> lore) {
        ItemStack item = material.parseItem();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes(`&`, name));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
