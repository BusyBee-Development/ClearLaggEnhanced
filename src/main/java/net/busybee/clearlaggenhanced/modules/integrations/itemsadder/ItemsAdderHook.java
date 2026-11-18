package net.busybee.clearlaggenhanced.modules.integrations.itemsadder;

import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemsAdderHook {

    private static final String PLUGIN_NAME = "ItemsAdder";
    private static final long LOOKUP_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final Logger logger;
    private volatile Plugin itemsAdder;
    private volatile long lastLookupNanos = System.nanoTime() - LOOKUP_INTERVAL_NANOS;
    private volatile boolean incompatible;
    private volatile boolean warnedFailure;

    public ItemsAdderHook(@NotNull Logger logger) {
        this.logger = logger;
    }

    public boolean isItemsAdderEntity(@NotNull Entity entity) {
        return isItemsAdderEntity(entity, true);
    }

    /**
     * @param keepOnFailure what to answer when ItemsAdder cannot check the entity: true for clears,
     *                      which keep what they cannot check, false for the mob limiter, which counts
     *                      it so a failing check can never switch the limit off
     */
    public boolean isItemsAdderEntity(@NotNull Entity entity, boolean keepOnFailure) {
        if (incompatible || !isItemsAdderEnabled()) {
            return false;
        }

        try {
            // ItemsAdder throws for any entity that cannot carry furniture, so only those are asked.
            if (canBeFurniture(entity) && CustomFurniture.byAlreadySpawned(entity) != null) {
                return true;
            }
            return CustomEntity.isCustomEntity(entity);
        } catch (LinkageError e) {
            // This ItemsAdder build does not have the API we compiled against; asking again cannot work.
            incompatible = true;
            logger.log(Level.WARNING, "ItemsAdder protection is off: this ItemsAdder version is not compatible with the hook.", e);
            return false;
        } catch (Exception e) {
            // ItemsAdder could not answer for this entity: clears keep it, the mob limiter counts it.
            if (!warnedFailure) {
                warnedFailure = true;
                logger.log(Level.WARNING, "ItemsAdder could not check an entity (" + entity.getType()
                        + "); clears keep entities it cannot check and the mob limiter counts them. Further failures are not logged.", e);
            }
            return keepOnFailure;
        }
    }

    // ItemsAdder furniture is always one of these; byAlreadySpawned throws for every other entity type.
    static boolean canBeFurniture(@NotNull Entity entity) {
        return entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof ItemDisplay;
    }

    // Runs once per entity during a clear, so a missing ItemsAdder is only looked up again every few seconds.
    private boolean isItemsAdderEnabled() {
        Plugin plugin = itemsAdder;
        if (plugin != null && plugin.isEnabled()) {
            return true;
        }

        long now = System.nanoTime();
        if (now - lastLookupNanos < LOOKUP_INTERVAL_NANOS) {
            return false;
        }
        lastLookupNanos = now;

        plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        itemsAdder = plugin;
        return plugin != null && plugin.isEnabled();
    }
}
