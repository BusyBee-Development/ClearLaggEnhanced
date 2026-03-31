package com.clearlagenhanced.commands;

import com.clearlagenhanced.modules.entityclearing.commands.ClearCommand;
import com.clearlagenhanced.modules.entityclearing.commands.NextCommand;
import com.clearlagenhanced.modules.chunkfinder.commands.ChunkFinderCommand;
import com.clearlagenhanced.modules.performance.commands.RamCommand;
import com.clearlagenhanced.modules.performance.commands.TpsCommand;
import org.bukkit.command.CommandSender;

public enum CommandRegistry {
    HELP("help", new HelpCommand()),
    CLEAR("clear", new ClearCommand()),
    NEXT("next", new NextCommand()),
    TPS("tps", new TpsCommand()),
    RAM("ram", new RamCommand()),
    CHUNKFINDER("chunkfinder", new ChunkFinderCommand()),
    ADMIN("admin", new AdminGUICommand()),
    RELOAD("reload", new ReloadCommand());

    private final String name;
    private final SubCommand executor;

    CommandRegistry(String name, SubCommand executor) {
        this.name = name;
        this.executor = executor;
    }

    public String getName() {
        return name;
    }

    public boolean execute(CommandSender sender, String[] args) {
        return executor.execute(sender, args);
    }

    public SubCommand getExecutor() {
        return executor;
    }

    public static CommandRegistry fromString(String name) {
        for (CommandRegistry cmd : values()) {
            if (cmd.name.equalsIgnoreCase(name)) {
                return cmd;
            }
        }
        return null;
    }

    public static String[] getCommandNames() {
        CommandRegistry[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name;
        }
        return names;
    }
}
