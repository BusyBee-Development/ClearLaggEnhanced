package com.clearlagenhanced.commands.subcommands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NextCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        long timeUntilNext = ClearLaggEnhanced.getInstance().getEntityManager().getTimeUntilNextClear();

        if (timeUntilNext == -1) {
            MessageUtils.sendMessage(sender, "next-clear.disabled");
            return true;
        }

        String formattedTime = ClearLaggEnhanced.getInstance().getEntityManager().getFormattedTimeUntilNextClear();
        Map<String, String> ph = new ConcurrentHashMap<>();
        ph.put("time", formattedTime);
        MessageUtils.sendMessage(sender, "next-clear.scheduled", ph);
        return true;
    }

    @Override
    public String getPermission() {
        return "CLE.next";
    }

    @Override
    public String getHelpMessageKey() {
        return "commands.help.next";
    }
}
