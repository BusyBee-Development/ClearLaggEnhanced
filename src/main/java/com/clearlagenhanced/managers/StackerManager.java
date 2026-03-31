package com.clearlagenhanced.managers;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.hooks.StackerHook;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StackerManager {

    private final ClearLaggEnhanced plugin;
    private final List<StackerHook> hooks = new ArrayList<>();

    public StackerManager(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
    }

    public void registerHook(StackerHook hook) {
        if (hook.isEnabled() && !hooks.contains(hook)) {
            hooks.add(hook);
            plugin.getLogger().info(hook.getName() + " hook registered");
        }
    }

    public void unregisterHook(StackerHook hook) {
        hooks.remove(hook);
        plugin.getLogger().info(hook.getName() + " hook unregistered");
    }

    public boolean isStacked(Entity entity) {
        if (entity instanceof Player) return false;
        return findHookFor(entity).isPresent();
    }

    public void removeStack(Entity entity) {
        if (entity instanceof Player) return;
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

    public List<StackerHook> getRegisteredHooks() {
        return new ArrayList<>(hooks);
    }
}