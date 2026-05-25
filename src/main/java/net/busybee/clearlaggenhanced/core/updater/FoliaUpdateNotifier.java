package net.busybee.clearlaggenhanced.core.updater;

import com.tcoded.folialib.FoliaLib;
import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import org.jetbrains.annotations.NotNull;

public class FoliaUpdateNotifier {

    private final ClearLaggEnhanced plugin;
    private final VersionCheck versionCheck;
    private final FoliaLib foliaLib;

    public FoliaUpdateNotifier(@NotNull ClearLaggEnhanced plugin, @NotNull VersionCheck versionCheck) {
        this.plugin = plugin;
        this.versionCheck = versionCheck;
        this.foliaLib = new FoliaLib(plugin);
    }

    public void check() {
        ClearLaggEnhanced.scheduler().runLater(() -> {
            if (!foliaLib.isFolia()) {
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
