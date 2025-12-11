package com.clearlagenhanced.hooks;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

public class WildStackerHook implements StackerHook {

    @Override
    public boolean isStacked(Entity entity) {
        if (entity instanceof LivingEntity) {
            StackedEntity stack = WildStackerAPI.getStackedEntity((LivingEntity) entity);
            return stack != null && stack.getStackAmount() > 1;
        } else if (entity instanceof Item) {
            StackedItem stack = WildStackerAPI.getStackedItem((Item) entity);
            return stack != null && stack.getStackAmount() > 1;
        }
        return false;
    }

    @Override
    public void removeStack(Entity entity) {
        if (entity instanceof LivingEntity) {
            StackedEntity stack = WildStackerAPI.getStackedEntity((LivingEntity) entity);
            if (stack != null) {
                int amount = stack.getStackAmount();
                Bukkit.getLogger().info("[WildStacker] Removing stacked entity " + entity.getType() + " with " + amount + " entities");
                // Remove the entire stack
                stack.setStackAmount(1, true);
                entity.remove();
                return;
            }
        } else if (entity instanceof Item) {
            StackedItem stack = WildStackerAPI.getStackedItem((Item) entity);
            if (stack != null) {
                int amount = stack.getStackAmount();
                Bukkit.getLogger().info("[WildStacker] Removing stacked item with " + amount + " items");
                // Remove the item entity which should clear the entire stack
                entity.remove();
                return;
            }
        }

        entity.remove();
    }
}
