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

        this.api = RoseStackerAPI.getInstance();
        return true;
    }

    @Override
    public boolean isStacked(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            StackedEntity stack = api.getStackedEntity(livingEntity);
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
        if (entity instanceof LivingEntity livingEntity) {
            StackedEntity stack = api.getStackedEntity(livingEntity);
            if (stack != null) {
                scheduler.runAtEntity(entity, task -> stack.getEntity().remove());
            }
        } else if (entity instanceof Item item) {
            StackedItem stack = api.getStackedItem(item);
            if (stack != null) {
                scheduler.runAtEntity(entity, task -> stack.getItem().remove());
            }
        }
    }
}
