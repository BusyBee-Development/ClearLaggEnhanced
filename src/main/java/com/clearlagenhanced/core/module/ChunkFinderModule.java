package com.clearlagenhanced.core.module;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.modules.performance.models.PerformanceManager;
import lombok.Getter;
import org.bukkit.entity.Player;

public class ChunkFinderModule extends Module {
    private final ClearLaggEnhanced plugin;
    @Getter private PerformanceManager performanceManager;

    public ChunkFinderModule(ClearLaggEnhanced plugin) {
        super("Chunk Finder", "chunk-finder");
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

    public void findLaggyChunksAsync(Player player) {
        if (performanceManager != null) {
            performanceManager.findLaggyChunksAsync(player);
        }
    }

    public void findLaggyChunks(Player player) {
        findLaggyChunksAsync(player);
    }
}
