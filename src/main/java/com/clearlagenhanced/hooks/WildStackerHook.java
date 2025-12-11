package com.clearlagenhanced.hooks;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

public class WildStackerHook implements StackerHook {

    @Override
    public boolean isStacked(Entity entity) {
        if (entity instanceof LivingEntity) {
            return WildStackerAPI.getStackedEntity((LivingEntity) entity) != null;
        } else if (entity instanceof Item) {
            return WildStackerAPI.getStackedItem((Item) entity) != null;
        }
        return false;
    }

    @Override
    public void removeStack(Entity entity) {
        if (entity instanceof LivingEntity) {
            StackedEntity stack = WildStackerAPI.getStackedEntity((LivingEntity) entity);
            if (stack != null) {
                stack.remove();
                return;
            }
        } else if (entity instanceof Item) {
            StackedItem stack = WildStackerAPI.getStackedItem((Item) entity);
            if (stack != null) {
                stack.remove();
                return;
            }
        }

        entity.remove();
    }
}
