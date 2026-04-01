package com.clearlagenhanced.core.module;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.modules.performance.models.PerformanceManager;
import lombok.Getter;

public class PerformanceModule extends Module {
    private final ClearLaggEnhanced plugin;
    @Getter private PerformanceManager performanceManager;

    public PerformanceModule(ClearLaggEnhanced plugin) {
        super("Performance", "performance");
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        performanceManager = new PerformanceManager(plugin);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onReload() {
        performanceManager = new PerformanceManager(plugin);
    }

    public double getTPS() {
        return performanceManager != null ? performanceManager.getTPS() : 20.0;
    }

    public String getFormattedMemoryUsage() {
        return performanceManager != null ? performanceManager.getFormattedMemoryUsage() : "N/A";
    }

    public double getMemoryUsagePercentage() {
        return performanceManager != null ? performanceManager.getMemoryUsagePercentage() : 0.0;
    }

    public int getTotalEntities() {
        return performanceManager != null ? performanceManager.getTotalEntities() : 0;
    }
}
