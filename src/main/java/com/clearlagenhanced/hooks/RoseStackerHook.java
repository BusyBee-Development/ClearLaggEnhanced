package com.clearlagenhanced.hooks;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.stack.StackedItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

public class RoseStackerHook implements StackerHook {

    private final RoseStackerAPI api;

    public RoseStackerHook() {
        this.api = RoseStackerAPI.getInstance();
    }

    @Override
    public boolean isStacked(Entity entity) {
        if (entity instanceof LivingEntity) {
            StackedEntity stack = api.getStackedEntity((LivingEntity) entity);
            return stack != null && stack.getStackSize() > 1;
        } else if (entity instanceof Item) {
            StackedItem stack = api.getStackedItem((Item) entity);
            return stack != null && stack.getStackSize() > 1;
        }
        return false;
    }

    @Override
    public void removeStack(Entity entity) {
        if (entity instanceof LivingEntity) {
            StackedEntity stack = api.getStackedEntity((LivingEntity) entity);
            if (stack != null) {
                int amount = stack.getStackSize();
                Bukkit.getLogger().info("[RoseStacker] Removing stacked entity " + entity.getType() + " with " + amount + " entities");
                // Remove the entity which will remove the entire stack
                stack.getEntity().remove();
            }
        } else if (entity instanceof Item) {
            StackedItem stack = api.getStackedItem((Item) entity);
            if (stack != null) {
                int amount = stack.getStackSize();
                Bukkit.getLogger().info("[RoseStacker] Removing stacked item with " + amount + " items");
                // Remove the item entity which should clear the entire stack
                stack.getItem().remove();
            }
        }
    }
}
