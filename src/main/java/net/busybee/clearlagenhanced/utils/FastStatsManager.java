package net.busybee.clearlagenhanced.utils;

import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.core.ErrorTracker;
import dev.faststats.core.data.Metric;
import net.busybee.clearlagenhanced.ClearLaggEnhanced;
import net.busybee.clearlagenhanced.modules.performance.models.PerformanceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class FastStatsManager {

    private final ClearLaggEnhanced plugin;
    private final BukkitMetrics metrics;

    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware()
            .anonymize("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "[uuid hidden]")
            .ignoreError(java.lang.reflect.InvocationTargetException.class);

    public FastStatsManager(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        String token = loadToken();

        this.metrics = BukkitMetrics.factory()
                .token(token)
                .errorTracker(ERROR_TRACKER)
                .addMetric(Metric.number("server_tps", () -> {
                    PerformanceManager pm = plugin.getPerformanceManager();
                    return pm != null ? pm.getTPS() : 20.0;
                }))
                .addMetric(Metric.number("entities_total", () -> {
                    PerformanceManager pm = plugin.getPerformanceManager();
                    return pm != null ? pm.getTotalEntities() : 0;
                }))
                .create(plugin);
    }

    private String loadToken() {
        Properties props = new Properties();
        try (InputStream is = plugin.getResource("faststats.properties")) {
            if (is != null) {
                props.load(is);
                return props.getProperty("token", "YOUR_TOKEN_HERE");
            }
        } catch (IOException ignored) {}
        return "YOUR_TOKEN_HERE";
    }

    public void onEnable() {
        metrics.ready();
        plugin.getLogger().info("FastStats metrics have been enabled!");
    }

    public void onDisable() {
        metrics.shutdown();
    }
}
