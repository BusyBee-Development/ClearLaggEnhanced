package com.clearlagenhanced.commands.subcommands;

import com.clearlagenhanced.commands.CommandRegistry;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class HelpCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        MessageUtils.sendMessage(sender, "commands.help.header");
        for (CommandRegistry cmd : CommandRegistry.values()) {
            if (sender.hasPermission(cmd.getExecutor().getPermission())) {
                MessageUtils.sendMessage(sender, cmd.getExecutor().getHelpMessageKey());
            }
        }

        return true;
    }

    @Override
    public String getPermission() {
        return "CLE.help";
    }

    @Override
    public String getHelpMessageKey() {
        return "commands.help.help";
    }
}
