package com.clearlagenhanced.hooks;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.stack.StackedItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

/**
 * Hook for RoseStacker integration.
 *
 * <p>RoseStacker does not inherently support Folia, but this hook is
 * implemented for parity reasons with non-Folia server environments.</p>
 */
public class RoseStackerHook implements StackerHook {

    private static final String PLUGIN_NAME = "RoseStacker";
    private RoseStackerAPI api;
    private final PlatformScheduler scheduler = ClearLaggEnhanced.scheduler();

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean isEnabled() {
        if (!Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME)) {
            return false;
        }

        try {
            this.api = RoseStackerAPI.getInstance();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isStacked(Entity entity) {
        if (api == null) return false;

        if (entity instanceof LivingEntity livingEntity) {
            StackedEntity stack = api.getStackedEntity(livingEntity);
            // FIX: Strictly require stack size > 1.
            // If size is 1, treat it as a normal entity so ClearLag can handle it normally.
            return stack != null && stack.getStackSize() > 1;
        }

        if (entity instanceof Item item) {
            StackedItem stack = api.getStackedItem(item);
            return stack != null && stack.getStackSize() > 1;
        }

        return false;
    }

    @Override
    public void removeStack(Entity entity) {
        if (api == null) {
            entity.remove();
            return;
        }

        scheduler.runAtEntity(entity, task -> {
            if (entity instanceof LivingEntity livingEntity) {
                StackedEntity stack = api.getStackedEntity(livingEntity);
                if (stack != null) {
                    api.removeEntityStack(stack);
                    if (livingEntity.isValid()) livingEntity.remove();
                } else {
                    livingEntity.remove();
                }
            } else if (entity instanceof Item item) {
                StackedItem stack = api.getStackedItem(item);
                if (stack != null) {
                    api.removeItemStack(stack);
                    if (item.isValid()) item.remove();
                } else {
                    item.remove();
                }
            } else {
                entity.remove();
            }
        });
    }
}
