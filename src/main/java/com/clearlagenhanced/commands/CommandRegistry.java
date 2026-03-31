package com.clearlagenhanced.commands;

import com.clearlagenhanced.commands.subcommands.*;
import com.clearlagenhanced.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@Getter
public enum CommandRegistry {

    HELP("help", new HelpCommand()),
    CLEAR("clear", new ClearCommand()),
    NEXT("next", new NextCommand()),
    TPS("tps", new TpsCommand()),
    RAM("ram", new RamCommand()),
    CHUNKFINDER("chunkfinder", new ChunkFinderCommand()),
    ADMIN("admin", new AdminCommand()),
    RELOAD("reload", new ReloadCommand());

    private final String name;
    private final SubCommand executor;

    CommandRegistry(@NotNull String name, @NotNull SubCommand executor) {
        this.name = name;
        this.executor = executor;
    }

    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(executor.getPermission())) {
            MessageUtils.sendMessage(sender, "notifications.no-permission");
            return true;
        }

        return executor.execute(sender, args);
    }

    public static CommandRegistry fromString(@NotNull String name) {
        for (CommandRegistry cmd : values()) {
            if (cmd.name.equalsIgnoreCase(name)) {
                return cmd;
            }
        }

        return null;
    }

    public static String[] getCommandNames() {
        CommandRegistry[] commands = values();
        String[] names = new String[commands.length];
        for (int i = 0; i < commands.length; i++) {
            names[i] = commands[i].name;
        }

        return names;
    }
}
