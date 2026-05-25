package net.busybee.clearlaggenhanced.modules.chunkfinder.inventory;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.modules.chunkfinder.ChunkFinderModule;
import net.busybee.clearlaggenhanced.gui.base.InventoryGUI;
import com.cryptomorin.xseries.XMaterial;
import com.tcoded.folialib.impl.PlatformScheduler;
import net.busybee.clearlaggenhanced.gui.impl.AdminGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

public class ChunkFinderGUI extends InventoryGUI {

    private final ClearLaggEnhanced plugin;
    private final ChunkFinderModule module;
    private final PlatformScheduler scheduler;

    public ChunkFinderGUI(ClearLaggEnhanced plugin, ChunkFinderModule module) {
        super(27, ChatColor.translateAlternateColorCodes('&', "&2Chunk Finder"));
        this.plugin = plugin;
        this.module = module;
        this.scheduler = ClearLaggEnhanced.scheduler();
    }

    @Override
    public void decorate(Player player) {
        ItemStack scanItem = XMaterial.COMPASS.parseItem();
        if (scanItem != null) {
            ItemMeta meta = scanItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aStart Scanning"));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Scan nearby chunks for laggy areas."));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to scan"));
                meta.setLore(lore);
                scanItem.setItemMeta(meta);
            }
        }

        setItem(13, scanItem, event -> {
            Player p = (Player) event.getWhoClicked();
            scheduler.runAtEntity(p, task -> {
                p.closeInventory();
                if (module != null) {
                    module.findLaggyChunksAsync(p);
                }
            });
        });

        ItemStack backItem = XMaterial.BARRIER.parseItem();
        if (backItem != null) {
            ItemMeta meta = backItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cBack"));
                backItem.setItemMeta(meta);
            }
        }

        setItem(22, backItem, event ->
            scheduler.runAtEntity(player, task ->
                new AdminGUI(plugin, plugin.getGuiRegistry()).open(player)
            )
        );
    }
}
