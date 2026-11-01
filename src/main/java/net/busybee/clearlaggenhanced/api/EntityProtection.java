package net.busybee.clearlaggenhanced.api;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether an entity belongs to your plugin and must not be removed.
 *
 * <p>Called for every candidate entity on the thread that owns it (on Folia, its region thread),
 * potentially thousands of times per clear, so keep it cheap and never block. Only touch the
 * entity you are given. If the check throws, the entity is kept.
 */
@FunctionalInterface
public interface EntityProtection {

    boolean isProtected(@NotNull Entity entity);

    /**
     * Whether the entities this check protects still count towards the mob limiter's per-chunk caps.
     *
     * <p>{@code false} (the default) suits entities your plugin controls itself, such as NPCs or
     * custom mobs: they neither count nor are blocked from spawning. Return {@code true} for
     * ordinary mobs you only want kept through clears, such as animals on a player's land, so
     * breeding and spawning there stay capped like everywhere else. Either way they are never removed.
     *
     * <p>Read once per check, so return a constant.
     */
    default boolean countsTowardsMobLimits() {
        return false;
    }
}
