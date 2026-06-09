package net.busybee.clearlaggenhanced.core.updater;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import org.bukkit.ChatColor;

public class UpdateWarn {

    // Set to false to disable this warning
    private static final boolean ENABLED = true;

    // Edit this array to change the message lines
    private static final String[] WARNING_MESSAGE = {
            "======================================================",
            "NOTICE: The following module folders will be removed",
            "in a future update as they are no longer needed:",
            " - WildStacker",
            " - RoseStacker",
            " - Modernshowcase",
            "For now the gui.yml is staying but the configs wont be.",
            "======================================================"
    };

    /**
     * Sends the warning message to the console if ENABLED is true.
     * @param plugin The main plugin instance
     */
    public static void sendWarning(ClearLaggEnhanced plugin) {
        if (!ENABLED) return;

        for (String line : WARNING_MESSAGE) {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.RED + line);
        }
    }
}
