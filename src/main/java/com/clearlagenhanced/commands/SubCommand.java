package com.clearlagenhanced.commands;

import org.bukkit.command.CommandSender;

public interface SubCommand {

    boolean execute(CommandSender sender, String[] args);

    String getPermission();

    String getHelpMessageKey();
}
