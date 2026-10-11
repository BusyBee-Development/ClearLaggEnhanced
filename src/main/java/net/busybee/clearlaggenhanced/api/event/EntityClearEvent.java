package net.busybee.clearlaggenhanced.api.event;

import net.busybee.clearlaggenhanced.api.ClearCause;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called before an entity clear removes anything. Cancelling skips this clear; an automatic clear
 * then waits for its next interval. Countdown warnings have already been broadcast by this point.
 *
 * <p>Fired off the main thread. To protect individual entities, register an
 * {@link net.busybee.clearlaggenhanced.api.EntityProtection} instead.
 */
public class EntityClearEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ClearCause cause;
    private boolean cancelled;

    public EntityClearEvent(@NotNull ClearCause cause, boolean async) {
        super(async);
        this.cause = cause;
    }

    public @NotNull ClearCause getCause() {
        return cause;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
