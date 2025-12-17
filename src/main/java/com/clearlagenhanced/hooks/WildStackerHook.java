package com.clearlagenhanced.hooks;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import com.clearlagenhanced.ClearLaggEnhanced;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

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
        if (!isEnabled()) return false;

        if (entity instanceof LivingEntity livingEntity) {
            StackedEntity stack = WildStackerAPI.getStackedEntity(livingEntity);
            return stack != null && stack.getStackAmount() > 1;
        }

        if (entity instanceof Item item) {
            StackedItem stack = WildStackerAPI.getStackedItem(item);
            return stack != null && stack.getStackAmount() > 1;
        }

        return false;
    }

    @Override
    public void removeStack(Entity entity) {
        if (!isEnabled()) {
            entity.remove();
            return;
        }

        if (entity instanceof LivingEntity livingEntity) {
            StackedEntity stack = WildStackerAPI.getStackedEntity(livingEntity);
            if (stack != null) {
                stack.remove();
            }
        } else if (entity instanceof Item item) {
            StackedItem stack = WildStackerAPI.getStackedItem(item);
            if (stack != null) {
                stack.remove();
            }
        }

        if (entity.isValid()) {
            entity.remove();
        }
    }
}