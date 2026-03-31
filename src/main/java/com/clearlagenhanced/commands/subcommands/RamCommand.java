package com.clearlagenhanced.commands.subcommands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RamCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        double memoryPercent = ClearLaggEnhanced.getInstance().getPerformanceManager().getMemoryUsagePercentage();

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        MessageUtils.sendMessage(sender, "performance.ram.header");
        Map<String, String> usedPh = new ConcurrentHashMap<>();
        usedPh.put("used", String.valueOf(usedMemory));
        usedPh.put("percent", String.format("%.1f", memoryPercent));
        MessageUtils.sendMessage(sender, "performance.ram.used", usedPh);
        Map<String, String> totalPh = new ConcurrentHashMap<>();
        totalPh.put("total", String.valueOf(totalMemory));
        MessageUtils.sendMessage(sender, "performance.ram.total", totalPh);
        Map<String, String> maxPh = new ConcurrentHashMap<>();
        maxPh.put("max", String.valueOf(maxMemory));
        MessageUtils.sendMessage(sender, "performance.ram.max", maxPh);
        return true;
    }

    @Override
    public String getPermission() {
        return "CLE.ram";
    }

    @Override
    public String getHelpMessageKey() {
        return "commands.help.ram";
    }
}
