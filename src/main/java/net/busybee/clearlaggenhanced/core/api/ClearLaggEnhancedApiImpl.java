package net.busybee.clearlaggenhanced.core.api;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.api.ClearCause;
import net.busybee.clearlaggenhanced.api.ClearLaggEnhancedAPI;
import net.busybee.clearlaggenhanced.api.ClearResult;
import net.busybee.clearlaggenhanced.api.EntityProtection;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.managers.EntityProtectionUtils;
import net.busybee.clearlaggenhanced.managers.ModuleManager;
import net.busybee.clearlaggenhanced.modules.entityclearing.EntityClearingModule;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

// Outlives /lagg reload, so it looks the managers up on every call instead of holding them.
public class ClearLaggEnhancedApiImpl implements ClearLaggEnhancedAPI, Listener {

    private final ClearLaggEnhanced plugin;
    @Getter private final ProtectionRegistry protections;

    public ClearLaggEnhancedApiImpl(@NotNull ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.protections = new ProtectionRegistry(plugin.getLogger());
    }

    @Override
    public void registerProtection(@NotNull Plugin owner, @NotNull EntityProtection protection) {
        protections.register(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(protection, "protection"));
    }

    @Override
    public void unregisterProtection(@NotNull EntityProtection protection) {
        protections.unregister(Objects.requireNonNull(protection, "protection"));
    }

    @Override
    public void unregisterProtections(@NotNull Plugin owner) {
        protections.unregisterAll(Objects.requireNonNull(owner, "owner"));
    }

    @Override
    public boolean isProtected(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "entity");
        EntityProtectionUtils utils = plugin.getEntityProtectionUtils();
        if (utils == null) {
            return entity instanceof Player || protections.isProtected(entity);
        }
        return utils.isProtected(entity, utils.createProtectionContext());
    }

    @Override
    public @NotNull CompletableFuture<ClearResult> clearEntities() {
        EntityClearingModule module = clearingModule();
        if (module == null || !plugin.isEnabled()) {
            return CompletableFuture.completedFuture(ClearResult.of(ClearResult.Status.UNAVAILABLE));
        }

        CompletableFuture<ClearResult> future = new CompletableFuture<>();
        ClearLaggEnhanced.scheduler().runAsync(task -> {
            try {
                future.complete(module.clearEntities(ClearCause.API));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public int getSecondsUntilNextClear() {
        EntityClearingModule module = clearingModule();
        return module == null ? -1 : (int) module.getTimeUntilNextClear();
    }

    @Override
    public boolean isModuleEnabled(@NotNull String module) {
        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) {
            return false;
        }
        Module found = moduleManager.getModule(module);
        return found != null && found.isEnabled();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        protections.unregisterAll(event.getPlugin());
    }

    private @Nullable EntityClearingModule clearingModule() {
        ModuleManager moduleManager = plugin.getModuleManager();
        if (moduleManager == null) {
            return null;
        }
        return moduleManager.getModule("entity-clearing") instanceof EntityClearingModule module && module.isEnabled()
                ? module
                : null;
    }
}
