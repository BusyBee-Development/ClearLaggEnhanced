package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.hooks.RoseStackerHook;
import com.clearlagenhanced.hooks.StackerHook;
import com.clearlagenhanced.hooks.WildStackerHook;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StackerManager {

    private final ClearLaggEnhanced plugin;
    private final List<StackerHook> hooks = new ArrayList<>();

    public StackerManager(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        registerHooks();
    }

    private void registerHooks() {
        registerHook(new RoseStackerHook());
        registerHook(new WildStackerHook());

        if (hooks.isEmpty()) {
            plugin.getLogger().info("No stacker plugins detected. Stack protection will work based on entity count only.");
        }
    }

    private void registerHook(StackerHook hook) {
        if (hook.isEnabled()) {
            hooks.add(hook);
            plugin.getLogger().info(hook.getName() + " hook enabled.");
        }
    }

    public boolean isStacked(Entity entity) {
        return findHookFor(entity).isPresent();
    }

    public void removeStack(Entity entity) {
        findHookFor(entity).ifPresent(hook -> {
            try {
                hook.removeStack(entity);
            } catch (Exception e) {
                plugin.getLogger().warning("Error removing stack: " + e.getMessage());
            }
        });
    }

    private Optional<StackerHook> findHookFor(Entity entity) {
        for (StackerHook hook : hooks) {
            try {
                if (hook.isStacked(entity)) {
                    return Optional.of(hook);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking if entity is stacked: " + e.getMessage());
            }
        }

        return Optional.empty();
    }
}
