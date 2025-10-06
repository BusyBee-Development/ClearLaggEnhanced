package com.clearlagenhanced.commands.subcommands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AdminCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage(sender, "errors.player-only");
            return true;
        }

        ClearLaggEnhanced.getInstance().getGUIManager().openMainGUI(player);
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
