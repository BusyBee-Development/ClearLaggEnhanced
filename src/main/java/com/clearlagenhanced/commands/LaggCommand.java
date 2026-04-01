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

<<<<<<< HEAD
=======
    private static final String NO_PERMISSION_MESSAGE_KEY = "notifications.no-permission";

>>>>>>> dev
    public LaggCommand() {
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
<<<<<<< HEAD
            CommandRegistry.HELP.execute(sender, new String[0]);
            return true;
=======
            return executeSubCommand(sender, CommandRegistry.HELP, new String[0]);
>>>>>>> dev
        }

        String subCommandName = args[0].toLowerCase();
        CommandRegistry subCommand = CommandRegistry.fromString(subCommandName);

        if (subCommand == null) {
            Map<String, String> ph = new ConcurrentHashMap<>();
            ph.put("sub", subCommandName);
            MessageUtils.sendMessage(sender, "commands.unknown-subcommand", ph);
<<<<<<< HEAD
            CommandRegistry.HELP.execute(sender, new String[0]);
            return true;
        }

        // Remove the subcommand name from args
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.execute(sender, subArgs);
=======

            if (canUse(sender, CommandRegistry.HELP.getExecutor())) {
                executeSubCommand(sender, CommandRegistry.HELP, new String[0]);
            }

            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return executeSubCommand(sender, subCommand, subArgs);
>>>>>>> dev
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
<<<<<<< HEAD
            String[] subCommands = CommandRegistry.getCommandNames();

            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
=======
            String input = args[0].toLowerCase();

            for (CommandRegistry subCommand : CommandRegistry.values()) {
                if (!canUse(sender, subCommand.getExecutor())) {
                    continue;
                }

                String commandName = subCommand.getName();
                if (commandName.toLowerCase().startsWith(input)) {
                    completions.add(commandName);
>>>>>>> dev
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }
<<<<<<< HEAD
=======

    private boolean executeSubCommand(@NotNull CommandSender sender, @NotNull CommandRegistry subCommand, @NotNull String[] args) {
        if (!canUse(sender, subCommand.getExecutor())) {
            MessageUtils.sendMessage(sender, NO_PERMISSION_MESSAGE_KEY);
            return true;
        }

        return subCommand.execute(sender, args);
    }

    private boolean canUse(@NotNull CommandSender sender, @NotNull SubCommand subCommand) {
        String permission = subCommand.getPermission();
        return permission == null || permission.isBlank() || sender.hasPermission(permission);
    }
>>>>>>> dev
}
