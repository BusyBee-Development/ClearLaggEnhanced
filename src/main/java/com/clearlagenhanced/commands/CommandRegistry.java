package com.clearlagenhanced.commands;

<<<<<<< HEAD
import com.clearlagenhanced.commands.subcommands.*;
import com.clearlagenhanced.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@Getter
public enum CommandRegistry {

=======
import com.clearlagenhanced.modules.entityclearing.commands.ClearCommand;
import com.clearlagenhanced.modules.entityclearing.commands.NextCommand;
import com.clearlagenhanced.modules.chunkfinder.commands.ChunkFinderCommand;
import com.clearlagenhanced.modules.performance.commands.RamCommand;
import com.clearlagenhanced.modules.performance.commands.TpsCommand;
import org.bukkit.command.CommandSender;

public enum CommandRegistry {
>>>>>>> dev
    HELP("help", new HelpCommand()),
    CLEAR("clear", new ClearCommand()),
    NEXT("next", new NextCommand()),
    TPS("tps", new TpsCommand()),
    RAM("ram", new RamCommand()),
    CHUNKFINDER("chunkfinder", new ChunkFinderCommand()),
<<<<<<< HEAD
    ADMIN("admin", new AdminCommand()),
=======
    ADMIN("admin", new AdminGUICommand()),
>>>>>>> dev
    RELOAD("reload", new ReloadCommand());

    private final String name;
    private final SubCommand executor;

<<<<<<< HEAD
    CommandRegistry(@NotNull String name, @NotNull SubCommand executor) {
=======
    CommandRegistry(String name, SubCommand executor) {
>>>>>>> dev
        this.name = name;
        this.executor = executor;
    }

<<<<<<< HEAD
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(executor.getPermission())) {
            MessageUtils.sendMessage(sender, "notifications.no-permission");
            return true;
        }

        return executor.execute(sender, args);
    }

    public static CommandRegistry fromString(@NotNull String name) {
=======
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
>>>>>>> dev
        for (CommandRegistry cmd : values()) {
            if (cmd.name.equalsIgnoreCase(name)) {
                return cmd;
            }
        }
<<<<<<< HEAD

=======
>>>>>>> dev
        return null;
    }

    public static String[] getCommandNames() {
<<<<<<< HEAD
        CommandRegistry[] commands = values();
        String[] names = new String[commands.length];
        for (int i = 0; i < commands.length; i++) {
            names[i] = commands[i].name;
        }

=======
        CommandRegistry[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name;
        }
>>>>>>> dev
        return names;
    }
}
