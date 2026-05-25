package net.busybee.clearlaggenhanced.modules.entityclearing.listeners;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.managers.EntityProtectionUtils;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class BreedingListener implements Listener {

    private final ClearLaggEnhanced plugin;

    public BreedingListener(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBreed(EntityBreedEvent event) {
        if (!plugin.getConfigManager().getBoolean("entity-clearing.extra-protections.mobs-from-breeding", true)) {
            return;
        }

        Entity offspring = event.getEntity();
        offspring.getPersistentDataContainer().set(EntityProtectionUtils.BRED_KEY, PersistentDataType.BYTE, (byte) 1);
    }
}
