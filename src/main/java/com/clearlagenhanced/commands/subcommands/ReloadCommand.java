package com.clearlagenhanced.commands.subcommands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        ClearLaggEnhanced.getInstance().reloadAll(sender);
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
