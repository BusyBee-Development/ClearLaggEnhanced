package com.clearlagenhanced.hooks;

import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.stack.StackedItem;
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
            return api.getStackedEntity((LivingEntity) entity) != null;
        } else if (entity instanceof Item) {
            return api.getStackedItem((Item) entity) != null;
        }
        return false;
    }

    @Override
    public void removeStack(Entity entity) {
        if (entity instanceof LivingEntity) {
            StackedEntity stack = api.getStackedEntity((LivingEntity) entity);
            if (stack != null) {
                // By removing the base Bukkit entity, RoseStacker's listeners will handle the stack logic.
                stack.getEntity().remove();
            }
        } else if (entity instanceof Item) {
            StackedItem stack = api.getStackedItem((Item) entity);
            if (stack != null) {
                // Same for stacked items.
                stack.getItem().remove();
            }
        }
    }
}
