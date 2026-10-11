package net.busybee.clearlaggenhanced.core.api;

import net.busybee.clearlaggenhanced.api.EntityProtection;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProtectionRegistry {

    private final Logger logger;
    // Read from every region thread during a clear.
    private final List<Registration> registrations = new CopyOnWriteArrayList<>();
    private final Set<EntityProtection> warnedFailures = ConcurrentHashMap.newKeySet();

    public ProtectionRegistry(@NotNull Logger logger) {
        this.logger = logger;
    }

    public void register(@NotNull Plugin owner, @NotNull EntityProtection protection) {
        for (Registration registration : registrations) {
            if (registration.protection() == protection) {
                return;
            }
        }
        registrations.add(new Registration(owner, protection));
    }

    public void unregister(@NotNull EntityProtection protection) {
        registrations.removeIf(registration -> registration.protection() == protection);
        warnedFailures.remove(protection);
    }

    public void unregisterAll(@NotNull Plugin owner) {
        registrations.removeIf(registration -> {
            if (!registration.owner().equals(owner)) {
                return false;
            }
            warnedFailures.remove(registration.protection());
            return true;
        });
    }

    public void clear() {
        registrations.clear();
        warnedFailures.clear();
    }

    public boolean isProtected(@NotNull Entity entity) {
        for (Registration registration : registrations) {
            try {
                if (registration.protection().isProtected(entity)) {
                    return true;
                }
            } catch (Exception | LinkageError e) {
                // A broken check must not cost another plugin its entities, so the entity is kept.
                if (warnedFailures.add(registration.protection())) {
                    logger.log(Level.WARNING, "Entity protection registered by " + registration.owner().getName()
                            + " failed; entities it could not check are kept. Further failures are not logged.", e);
                }
                return true;
            }
        }
        return false;
    }

    private record Registration(@NotNull Plugin owner, @NotNull EntityProtection protection) {
    }
}
