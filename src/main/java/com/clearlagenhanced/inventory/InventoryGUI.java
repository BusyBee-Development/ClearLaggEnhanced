package com.clearlagenhanced.inventory;

import fr.mrmicky.fastinv.FastInv;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public abstract class InventoryGUI extends FastInv {
    
    public InventoryGUI(int size, String title) {
        super(size, title);
    }
    public abstract void decorate(Player player);

    @Override
    protected void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @Override
    protected void onOpen(InventoryOpenEvent event) {
        decorate((Player) event.getPlayer());
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
    }
}
