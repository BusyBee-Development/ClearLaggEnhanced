package com.clearlagenhanced.hooks;

import com.clearlagenhanced.ClearLaggEnhanced;
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
        return "clearlagenhanced";
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
        if (plugin.getPerformanceManager() == null) {
            return null;
        }

        return switch (params.toLowerCase()) {
            case "tps" -> String.format("%.2f", plugin.getPerformanceManager().getTPS());
            case "memory_used" -> String.valueOf(plugin.getPerformanceManager().getUsedMemory() / 1024 / 1024);
            case "memory_max" -> String.valueOf(plugin.getPerformanceManager().getMaxMemory() / 1024 / 1024);
            case "memory_percentage" -> String.format("%.1f", plugin.getPerformanceManager().getMemoryUsagePercentage());
            case "entities_total" -> String.valueOf(plugin.getPerformanceManager().getTotalEntities());
            case "next_clear" -> String.valueOf(plugin.getEntityManager() != null
                ? plugin.getEntityManager().getTimeUntilNextClear()
                : 0);
            default -> null;
        };
    }
}
