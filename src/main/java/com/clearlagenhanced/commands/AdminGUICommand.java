package com.clearlagenhanced.commands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.inventory.impl.AdminGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminGUICommand implements SubCommand {

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

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
