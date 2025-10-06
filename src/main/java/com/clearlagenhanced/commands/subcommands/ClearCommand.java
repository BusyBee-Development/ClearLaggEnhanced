package com.clearlagenhanced.commands.subcommands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClearCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        MessageUtils.sendMessage(sender, "commands.clear.starting");

        ClearLaggEnhanced.scheduler().runAsync(task -> {
            long startTime = System.currentTimeMillis();
            int cleared = ClearLaggEnhanced.getInstance().getEntityManager().clearEntities();
            long duration = System.currentTimeMillis() - startTime;

            Map<String, String> ph = new ConcurrentHashMap<>();
            ph.put("count", String.valueOf(cleared));
            ph.put("time", String.valueOf(duration));
            MessageUtils.sendMessage(sender, "notifications.clear-complete", ph);
        });

        return true;
    }

    @Override
    public String getPermission() {
        return "CLE.clear";
    }

    @Override
    public String getHelpMessageKey() {
        return "commands.help.clear";
    }
}
