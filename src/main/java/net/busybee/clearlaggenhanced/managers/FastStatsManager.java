package net.busybee.clearlaggenhanced.managers;

import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.data.Metric;
import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.modules.performance.PerformanceModule;
import net.busybee.clearlaggenhanced.modules.performance.models.PerformanceManager;

public class FastStatsManager {

    private static final String FASTSTATS_TOKEN = "90b9a58974a453e2f3c2a00860e15641";

    private final ClearLaggEnhanced plugin;
    private final ModuleManager moduleManager;
    private final BukkitMetrics metrics;

    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware()
            .anonymize("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "[uuid hidden]")
            .ignoreError(java.lang.reflect.InvocationTargetException.class);

    public FastStatsManager(ClearLaggEnhanced plugin, ModuleManager moduleManager) {
        this.plugin = plugin;
        this.moduleManager = moduleManager;

        this.metrics = BukkitMetrics.factory()
                .token(FASTSTATS_TOKEN)
                .errorTracker(ERROR_TRACKER)
                .addMetric(Metric.number("server_tps", () -> {
                    PerformanceManager pm = getPerformanceManager();
                    return pm != null ? pm.getTPS() : 20.0;
                }))
                .addMetric(Metric.number("entities_total", () -> {
                    PerformanceManager pm = getPerformanceManager();
                    return pm != null ? pm.getTotalEntities() : 0;
                }))
                .create(plugin);
    }

    private PerformanceManager getPerformanceManager() {
        Module module = moduleManager.getModule("Performance");
        return module instanceof PerformanceModule performanceModule ? performanceModule.getPerformanceManager() : null;
    }

    public void onEnable() {
        metrics.ready();
    }
    public void onDisable() {
        metrics.shutdown();
    }
}
