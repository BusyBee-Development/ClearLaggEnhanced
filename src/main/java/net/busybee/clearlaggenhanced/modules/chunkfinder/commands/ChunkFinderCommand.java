package net.busybee.clearlaggenhanced.modules.chunkfinder.commands;

import net.busybee.clearlaggenhanced.ClearLaggEnhanced;
import net.busybee.clearlaggenhanced.commands.SubCommand;
import net.busybee.clearlaggenhanced.modules.chunkfinder.ChunkFinderModule;
import net.busybee.clearlaggenhanced.utils.MessageUtils;
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
