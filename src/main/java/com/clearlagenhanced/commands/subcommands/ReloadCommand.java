package com.clearlagenhanced.commands.subcommands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        ClearLaggEnhanced.getInstance().reloadAll();
        MessageUtils.sendMessage(sender, "notifications.reload-complete");
        return true;
    }

    @Override
    public String getPermission() {
        return "CLE.reload";
    }

    @Override
    public String getHelpMessageKey() {
        return "commands.help.reload";
    }
}
