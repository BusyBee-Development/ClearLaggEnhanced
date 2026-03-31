package com.clearlagenhanced.modules.integrations.wildstacker;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.hooks.StackerHook;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

/**
 * Hook for WildStacker integration.
 *
 * <p>WildStacker does not inherently support Folia, but this hook is
 * implemented for parity reasons with non-Folia server environments.</p>
 */
public class WildStackerHook implements StackerHook {

    private static final String PLUGIN_NAME = "WildStacker";
    private final PlatformScheduler scheduler = ClearLaggEnhanced.scheduler();

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
    }

    @Override
    public boolean isStacked(Entity entity) {
        // WildStacker integration is not available due to missing external dependency
        return false;
    }

    @Override
    public void removeStack(Entity entity) {
        // WildStacker integration is not available due to missing external dependency
        entity.remove();
    }
}
