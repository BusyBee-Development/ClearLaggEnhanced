
package com.clearlagenhanced.hooks;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.core.module.EntityClearingModule;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;


public class ClearLaggEnhancedExpansion extends PlaceholderExpansion {

    private final ClearLaggEnhanced plugin;

    public ClearLaggEnhancedExpansion(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "clearlaggenhanced";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return switch (params.toLowerCase()) {
            case "tps" -> {
                var pm = plugin.getPerformanceManager();
                yield pm != null ? String.format("%.2f", pm.getTPS()) : "0.00";
            }
            case "memory_used" -> {
                var pm = plugin.getPerformanceManager();
                yield pm != null ? String.valueOf(pm.getUsedMemory() / 1024 / 1024) : "0";
            }
            case "memory_max" -> {
                var pm = plugin.getPerformanceManager();
                yield pm != null ? String.valueOf(pm.getMaxMemory() / 1024 / 1024) : "0";
            }
            case "memory_percentage" -> {
                var pm = plugin.getPerformanceManager();
                yield pm != null ? String.format("%.1f", pm.getMemoryUsagePercentage()) : "0.0";
            }
            case "entities_total" -> {
                var pm = plugin.getPerformanceManager();
                yield pm != null ? String.valueOf(pm.getTotalEntities()) : "0";
            }
            case "next_clear" -> {
                EntityClearingModule module = (EntityClearingModule) plugin.getModuleManager().getModule("entity-clearing");
                yield String.valueOf(module != null ? module.getTimeUntilNextClear() : 0);
            }
            case "next_clear_formatted" -> {
                EntityClearingModule module = (EntityClearingModule) plugin.getModuleManager().getModule("entity-clearing");
                yield module != null ? module.getFormattedTimeUntilNextClear() : "0s";
            }
            default -> null;
        };
    }
}
