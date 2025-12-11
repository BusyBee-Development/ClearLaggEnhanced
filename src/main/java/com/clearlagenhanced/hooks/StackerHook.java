package com.clearlagenhanced.hooks;

import org.bukkit.entity.Entity;

public interface StackerHook {
    boolean isStacked(Entity entity);
    void removeStack(Entity entity);
}
