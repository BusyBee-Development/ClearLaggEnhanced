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
    private final ClearLaggEnhanced plugin;
    private final List<StackerHook> hooks = new ArrayList<>();

    public StackerManager(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().isPluginEnabled("RoseStacker")) {
            hooks.add(new RoseStackerHook());
            plugin.getLogger().info("RoseStacker hook enabled.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
            hooks.add(new WildStackerHook());
            plugin.getLogger().info("WildStacker hook enabled.");
        }

        if (hooks.isEmpty()) {
            plugin.getLogger().info("No stacker plugins detected. Stack protection will work based on entity count only.");
        }
    }

    public boolean isStacked(Entity entity) {
        // If no stacker plugins are loaded, no entities are considered stacked
        if (hooks.isEmpty()) {
            return false;
        }

        for (StackerHook hook : hooks) {
            try {
                if (hook.isStacked(entity)) {
                    if (plugin.getConfigManager().getBoolean("debug.entity-clearing", false)) {
                        plugin.getLogger().info("Entity " + entity.getType() + " is stacked (detected by " + hook.getClass().getSimpleName() + ")");
                    }
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking if entity is stacked: " + e.getMessage());
            }
        }
        return false;
    }

    public void removeStack(Entity entity) {
        for (StackerHook hook : hooks) {
            try {
                if (hook.isStacked(entity)) {
                    hook.removeStack(entity);
                    return; // Assume only one hook will handle the stack
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error removing stack: " + e.getMessage());
            }
        }
    }
}
