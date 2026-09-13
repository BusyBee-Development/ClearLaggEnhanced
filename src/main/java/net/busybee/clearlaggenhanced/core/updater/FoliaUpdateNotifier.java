package net.busybee.clearlaggenhanced.core.updater;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.core.scheduler.PluginScheduler;
import org.jetbrains.annotations.NotNull;

public class FoliaUpdateNotifier {

    private final ClearLaggEnhanced plugin;
    private final VersionCheck versionCheck;

    public FoliaUpdateNotifier(@NotNull ClearLaggEnhanced plugin, @NotNull VersionCheck versionCheck) {
        this.plugin = plugin;
        this.versionCheck = versionCheck;
    }

    public void check() {
        ClearLaggEnhanced.scheduler().runLater(() -> {
            if (!PluginScheduler.isFolia()) {
                return;
            }

            if (versionCheck.isUpdateAvailable()) {
                String latest = versionCheck.getLatestVersion();
                String current = plugin.getDescription().getVersion();

                plugin.getLogger().warning("======================================================");
                plugin.getLogger().warning("[ClearLaggEnhanced] Folia Update Available!");
                plugin.getLogger().warning("Current Version: " + current);
                plugin.getLogger().warning("Latest Version: " + latest);
                plugin.getLogger().warning("");
                plugin.getLogger().warning("It is highly recommended to update for optimal");
                plugin.getLogger().warning("performance and Folia-specific stability fixes.");
                plugin.getLogger().warning("Download: https://modrinth.com/plugin/clearlaggenhanced");
                plugin.getLogger().warning("======================================================");
            }
        }, 100L);
    }
}
