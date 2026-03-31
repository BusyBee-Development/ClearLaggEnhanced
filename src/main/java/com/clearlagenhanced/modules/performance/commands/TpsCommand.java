package com.clearlagenhanced.modules.performance.commands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.core.module.PerformanceModule;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TpsCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        PerformanceModule module = (PerformanceModule) ClearLaggEnhanced.getInstance()
                .getModuleManager().getModule("performance");
        
        if (module == null || !module.isEnabled()) {
            sender.sendMessage("Performance module is not enabled!");
            return true;
        }

        double tps = module.getTPS();
        Map<String, String> ph = new ConcurrentHashMap<>();
        ph.put("tps", String.format("%.2f", tps));
        MessageUtils.sendMessage(sender, "performance.tps", ph);
        return true;
    }

    @Override
    public String getPermission() {
        return "CLE.tps";
    }

    @Override
    public String getHelpMessageKey() {
        return "commands.help.tps";
    }
}