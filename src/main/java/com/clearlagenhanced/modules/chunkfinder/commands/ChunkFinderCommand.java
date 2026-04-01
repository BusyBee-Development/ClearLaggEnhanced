package com.clearlagenhanced.modules.chunkfinder.commands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.core.module.ChunkFinderModule;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChunkFinderCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMessage(sender, "errors.player-only");
            return true;
        }

        ChunkFinderModule module = (ChunkFinderModule) ClearLaggEnhanced.getInstance()
                .getModuleManager().getModule("chunk-finder");
        
        if (module == null || !module.isEnabled()) {
            sender.sendMessage("Chunk finder module is not enabled!");
            return true;
        }

        module.findLaggyChunksAsync(player);
        return true;
    }

    @Override
    public String getPermission() {
        return "CLE.chunkfinder";
    }

    @Override
    public String getHelpMessageKey() {
        return "commands.help.chunkfinder";
    }
}
