package net.busybee.clearlaggenhanced.api;

public enum ClearCause {
    /** The automatic clear timer. */
    AUTOMATIC,
    /** {@code /lagg clear}. */
    COMMAND,
    /** {@link ClearLaggEnhancedAPI#clearEntities()}. */
    API
}
