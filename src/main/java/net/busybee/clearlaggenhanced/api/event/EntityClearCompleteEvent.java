package net.busybee.clearlaggenhanced.api.event;

import net.busybee.clearlaggenhanced.api.ClearCause;
import net.busybee.clearlaggenhanced.api.ClearResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called after an entity clear has finished. Not called for clears that were cancelled or never started.
 *
 * <p>Fired off the main thread.
 */
public class EntityClearCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ClearCause cause;
    private final ClearResult result;

    public EntityClearCompleteEvent(@NotNull ClearCause cause, @NotNull ClearResult result, boolean async) {
        super(async);
        this.cause = cause;
        this.result = result;
    }

    public @NotNull ClearCause getCause() {
        return cause;
    }

    public @NotNull ClearResult getResult() {
        return result;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
