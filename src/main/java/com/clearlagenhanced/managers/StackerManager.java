package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.hooks.RoseStackerHook;
import com.clearlagenhanced.hooks.StackerHook;
import com.clearlagenhanced.hooks.WildStackerHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class StackerManager {
    private final List<StackerHook> hooks = new ArrayList<>();

    public StackerManager(ClearLaggEnhanced plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("RoseStacker")) {
            hooks.add(new RoseStackerHook());
            plugin.getLogger().info("RoseStacker hook enabled.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
            hooks.add(new WildStackerHook());
            plugin.getLogger().info("WildStacker hook enabled.");
        }
    }

    public boolean isStacked(Entity entity) {
        for (StackerHook hook : hooks) {
            if (hook.isStacked(entity)) {
                return true;
            }
        }
        return false;
    }

    public void removeStack(Entity entity) {
        for (StackerHook hook : hooks) {
            if (hook.isStacked(entity)) {
                hook.removeStack(entity);
                return; // Assume only one hook will handle the stack
            }
        }
    }
}
