package net.busybee.clearlaggenhanced.modules.integrations.griefprevention3d;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class GriefPrevention3DHook {

    private final boolean enabled;
    private Object dataStore;
    private Method getClaimAtMethod;

    public GriefPrevention3DHook(Plugin plugin) {
        boolean isEnabled = false;
        if (plugin != null && plugin.isEnabled()) {
            try {
                Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
                Object gpInstance = gpClass.getField("instance").get(null);
                dataStore = gpClass.getField("dataStore").get(gpInstance);

                // getClaimAt(Location, boolean, Claim)
                Class<?> claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
                getClaimAtMethod = dataStore.getClass().getMethod("getClaimAt", Location.class, boolean.class, claimClass);

                isEnabled = true;
            } catch (Exception e) {
                Bukkit.getLogger().warning("[ClearLaggEnhanced] Failed to hook into GriefPrevention: " + e.getMessage());
            }
        }
        this.enabled = isEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isInsideClaim(Entity entity) {
        if (!enabled || entity == null) return false;
        try {
            Object claim = getClaimAtMethod.invoke(dataStore, entity.getLocation(), false, null);
            return claim != null;
        } catch (Exception e) {
            return false;
        }
    }
}
