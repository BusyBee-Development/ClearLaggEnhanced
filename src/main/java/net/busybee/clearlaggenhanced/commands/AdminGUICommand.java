package net.busybee.clearlaggenhanced.commands;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.gui.impl.AdminGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminGUICommand implements SubCommand {

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        ClearLaggEnhanced plugin = ClearLaggEnhanced.getInstance();
        AdminGUI adminGUI = new AdminGUI(plugin, plugin.getGuiRegistry());
        adminGUI.open(player);
        return true;
    }

    @Override
    public String getPermission() {
        return "CLE.admin";
    }

    @Override
    public String getHelpMessageKey() {
        return "commands.help.admin";
    }
}
