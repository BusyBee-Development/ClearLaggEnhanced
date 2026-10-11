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
}
