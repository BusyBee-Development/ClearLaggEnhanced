package com.clearlagenhanced.modules.integrations.wildstacker;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.hooks.StackerHook;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

/**
 * Hook for WildStacker integration.
 *
 * <p>WildStacker does not inherently support Folia, so stack removal is always
 * executed through the scheduler on the entity thread for parity with the
 * plugin's other stacker integrations.</p>
 */
public class WildStackerHook implements StackerHook {

    private static final String PLUGIN_NAME = "WildStacker";
    private final PlatformScheduler scheduler = ClearLaggEnhanced.scheduler();

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }

    @Override
    public boolean isStacked(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            StackedEntity stackedEntity = WildStackerAPI.getStackedEntity(livingEntity);
            return stackedEntity != null && stackedEntity.getStackAmount() > 1;
        }

        if (entity instanceof Item item) {
            StackedItem stackedItem = WildStackerAPI.getStackedItem(item);
            return stackedItem != null && stackedItem.getStackAmount() > 1;
        }

        return false;
    }

    @Override
    public void removeStack(Entity entity) {
        scheduler.runAtEntity(entity, task -> {
            if (entity instanceof LivingEntity livingEntity) {
                removeLivingEntityStack(livingEntity);
                return;
            }

            if (entity instanceof Item item) {
                removeItemStack(item);
                return;
            }

            entity.remove();
        });
    }

    private void removeLivingEntityStack(LivingEntity entity) {
        StackedEntity stackedEntity = WildStackerAPI.getStackedEntity(entity);
        if (stackedEntity != null) {
            stackedEntity.remove();
            return;
        }

        entity.remove();
    }

    private void removeItemStack(Item item) {
        StackedItem stackedItem = WildStackerAPI.getStackedItem(item);
        if (stackedItem != null) {
            stackedItem.remove();
            return;
        }

        item.remove();
    }
}
