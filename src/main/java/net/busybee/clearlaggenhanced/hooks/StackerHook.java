package net.busybee.clearlaggenhanced.hooks;

import org.bukkit.entity.Entity;

public interface StackerHook {

    String getName();

    boolean isEnabled();

    boolean isStacked(Entity entity);

    void removeStack(Entity entity);
}
