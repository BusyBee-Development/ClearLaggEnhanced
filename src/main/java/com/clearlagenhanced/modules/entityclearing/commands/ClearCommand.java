package com.clearlagenhanced.modules.entityclearing.commands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.core.module.EntityClearingModule;
import com.clearlagenhanced.utils.MessageUtils;
import com.tcoded.folialib.impl.PlatformScheduler;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClearCommand implements SubCommand {

    private final PlatformScheduler scheduler = ClearLaggEnhanced.scheduler();

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        EntityClearingModule module = (EntityClearingModule) ClearLaggEnhanced.getInstance()
                .getModuleManager().getModule("entity-clearing");
        
        if (module == null || !module.isEnabled()) {
            sender.sendMessage("Entity clearing module is not enabled!");
            return true;
        }

        MessageUtils.sendMessage(sender, "commands.clear.starting");

        scheduler.runAsync(task -> {
            long startTime = System.currentTimeMillis();
            int cleared = module.clearEntities();
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
