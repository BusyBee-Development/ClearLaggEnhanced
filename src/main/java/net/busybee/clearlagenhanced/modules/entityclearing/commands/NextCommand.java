package net.busybee.clearlagenhanced.modules.entityclearing.commands;

import net.busybee.clearlagenhanced.ClearLaggEnhanced;
import net.busybee.clearlagenhanced.commands.SubCommand;
import net.busybee.clearlagenhanced.core.module.EntityClearingModule;
import net.busybee.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NextCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        EntityClearingModule module = (EntityClearingModule) ClearLaggEnhanced.getInstance()
                .getModuleManager().getModule("entity-clearing");
        
        if (module == null || !module.isEnabled()) {
            MessageUtils.sendMessage(sender, "next-clear.disabled");
            return true;
        }

        long timeUntilNext = module.getTimeUntilNextClear();

        if (timeUntilNext == -1) {
            MessageUtils.sendMessage(sender, "next-clear.disabled");
            return true;
        }

        String formattedTime = module.getFormattedTimeUntilNextClear();
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
