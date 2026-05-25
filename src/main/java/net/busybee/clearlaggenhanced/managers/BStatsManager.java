package net.busybee.clearlaggenhanced.managers;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import org.bstats.bukkit.Metrics;

public class BStatsManager {

    private static final int BSTATS_ID = 26743;

    public BStatsManager(ClearLaggEnhanced plugin) {
        new Metrics(plugin, BSTATS_ID);
    }
}
