package net.busybee.clearlaggenhanced.modules.performance;
import net.busybee.clearlaggenhanced.core.Module;
import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.modules.performance.inventory.PerformanceGUI;
import net.busybee.clearlaggenhanced.modules.performance.models.PerformanceManager;
import lombok.Getter;

public class PerformanceModule extends Module {
    private final ClearLaggEnhanced plugin;
    @Getter private PerformanceManager performanceManager;

    public PerformanceModule(ClearLaggEnhanced plugin) {
        super("Performance", "performance");
        this.plugin = plugin;
    }

    @Override
    public void onRegister() {
        registerGUI("performance", "Performance", "CLOCK", () -> new PerformanceGUI(plugin, this));
    }

    @Override
    public void onEnable() {
        performanceManager = new PerformanceManager(plugin);
        performanceManager.start();
    }

    @Override
    public void onDisable() {
        if (performanceManager != null) {
            performanceManager.stop();
        }
    }

    @Override
    public void onReload() {
        if (performanceManager != null) {
            performanceManager.stop();
        }
        performanceManager = new PerformanceManager(plugin);
        performanceManager.start();
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
