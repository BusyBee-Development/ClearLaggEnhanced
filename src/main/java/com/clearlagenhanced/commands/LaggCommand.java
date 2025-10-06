package com.clearlagenhanced.commands;

import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LaggCommand implements CommandExecutor, TabCompleter {

    public LaggCommand() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            CommandRegistry.HELP.execute(sender, new String[0]);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        CommandRegistry subCommand = CommandRegistry.fromString(subCommandName);

        if (subCommand == null) {
            Map<String, String> ph = new ConcurrentHashMap<>();
            ph.put("sub", subCommandName);
            MessageUtils.sendMessage(sender, "commands.unknown-subcommand", ph);
            CommandRegistry.HELP.execute(sender, new String[0]);
            return true;
        }

        // Remove the subcommand name from args
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.execute(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String[] subCommands = CommandRegistry.getCommandNames();

            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }
}
