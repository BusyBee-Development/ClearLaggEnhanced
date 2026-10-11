package net.busybee.clearlaggenhanced.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Entry point for plugins that hook into ClearLaggEnhanced.
 *
 * <p>Add {@code ClearLaggEnhanced} to {@code depend} or {@code softdepend} in your plugin.yml, then
 * call {@link #get()} from {@code onEnable} or later. The instance stays valid across {@code /lagg reload}.
 *
 * <p>Only the {@code net.busybee.clearlaggenhanced.api} package is API. Everything else in the jar
 * can change between versions without notice.
 */
public interface ClearLaggEnhancedAPI {

    /**
     * @throws IllegalStateException if ClearLaggEnhanced is not enabled
     */
    static @NotNull ClearLaggEnhancedAPI get() {
        ClearLaggEnhancedAPI api = Bukkit.getServicesManager().load(ClearLaggEnhancedAPI.class);
        if (api == null) {
            throw new IllegalStateException("ClearLaggEnhanced is not enabled");
        }
        return api;
    }

    /**
     * Adds a check that keeps entities from being removed by entity clearing and by the misc entity
     * limiter, whose per-chunk caps neither remove nor count them. Protected entities also stop
     * counting towards the mob limiter, like every other protected entity.
     *
     * <p>The misc entity limiter asks when an entity is created, so mark yours before it spawns.
     *
     * <p>Registrations are dropped when {@code owner} is disabled. Registering the same instance twice does nothing.
     */
    void registerProtection(@NotNull Plugin owner, @NotNull EntityProtection protection);

    void unregisterProtection(@NotNull EntityProtection protection);

    void unregisterProtections(@NotNull Plugin owner);

    /**
     * Whether entity clearing would keep this entity: the owner's protection settings and whitelist,
     * plus every registered {@link EntityProtection}. Players are always protected.
     *
     * <p>Reads entity state, so call it on the thread that owns the entity (on Folia, its region thread).
     */
    boolean isProtected(@NotNull Entity entity);

    /**
     * Runs an entity clear now, the same as {@code /lagg clear}, without broadcasting the result.
     * Safe to call from any thread; the future completes off the main thread.
     */
    @NotNull CompletableFuture<ClearResult> clearEntities();

    /**
     * @return seconds until the next automatic clear, or {@code -1} when automatic clearing is not running
     */
    int getSecondsUntilNextClear();

    /**
     * @param module the module's folder name, e.g. {@code entity-clearing} or {@code mob-limiter}
     * @return false when the module is off or unknown
     */
    boolean isModuleEnabled(@NotNull String module);
}
