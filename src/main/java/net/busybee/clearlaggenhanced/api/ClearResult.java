package net.busybee.clearlaggenhanced.api;

import org.jetbrains.annotations.NotNull;

/**
 * Outcome of one entity clear. The counts are zero unless {@link #completed()}.
 *
 * @param cleared entities removed (a stack counts once)
 * @param skipped entities kept because something protects them
 * @param chunks loaded chunks that were scanned
 */
public record ClearResult(@NotNull Status status, int cleared, int skipped, int chunks, long durationMillis) {

    public enum Status {
        COMPLETED,
        /** Another clear was still running. */
        ALREADY_RUNNING,
        /** A plugin cancelled the {@link net.busybee.clearlaggenhanced.api.event.EntityClearEvent}. */
        CANCELLED,
        /** The Entity Clearing module is off. */
        UNAVAILABLE
    }

    public static @NotNull ClearResult of(@NotNull Status status) {
        return new ClearResult(status, 0, 0, 0, 0L);
    }

    public boolean completed() {
        return status == Status.COMPLETED;
    }
}
