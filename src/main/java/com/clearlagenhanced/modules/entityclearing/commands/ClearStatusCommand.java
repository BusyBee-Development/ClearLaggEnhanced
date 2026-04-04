package com.clearlagenhanced.modules.entityclearing.commands;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.clearlagenhanced.commands.SubCommand;
import com.clearlagenhanced.core.module.EntityClearingModule;
import com.clearlagenhanced.modules.entityclearing.models.AdaptiveIntervalSettings;
import com.clearlagenhanced.modules.entityclearing.tasks.AutoClearTask;
import com.clearlagenhanced.utils.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ClearStatusCommand implements SubCommand {

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
        EntityClearingModule module = (EntityClearingModule) ClearLaggEnhanced.getInstance()
                .getModuleManager().getModule("entity-clearing");

        if (module == null || !module.isEnabled()) {
            MessageUtils.sendMessage(sender, "next-clear.disabled");
            return true;
        }

        AutoClearTask.StatusSnapshot status = module.getStatusSnapshot();
        if (status == null) {
            MessageUtils.sendMessage(sender, "next-clear.disabled");
            return true;
        }

        MessageUtils.sendMessage(sender, "commands.clearstatus.header");
        MessageUtils.sendMessage(sender, "commands.clearstatus.mode", Map.of(
                "mode", status.adaptiveEnabled() ? "adaptive" : "fixed",
                "metric", formatMetric(status.metric())
        ));

        if (status.sampledMetricValue() >= 0) {
            MessageUtils.sendMessage(sender, "commands.clearstatus.detected", Map.of(
                    "value", String.valueOf(status.sampledMetricValue()),
                    "label", metricLabel(status.metric())
            ));
        } else {
            MessageUtils.sendMessage(sender, "commands.clearstatus.detected-unavailable");
        }

        MessageUtils.sendMessage(sender, "commands.clearstatus.current-interval", Map.of(
                "interval", String.valueOf(status.activeIntervalSeconds())
        ));

        MessageUtils.sendMessage(sender, "commands.clearstatus.fallback-interval", Map.of(
                "interval", String.valueOf(status.fallbackIntervalSeconds())
        ));

        AutoClearTask.PerformanceGateStatus performanceGateStatus = status.performanceGateStatus();
        if (performanceGateStatus.enabled()) {
            MessageUtils.sendMessage(sender, "commands.clearstatus.performance-gate-header");
            if (performanceGateStatus.metricUnavailable()) {
                MessageUtils.sendMessage(sender, "commands.clearstatus.performance-gate-unavailable");
            } else {
                MessageUtils.sendMessage(sender, "commands.clearstatus.performance-gate-current", Map.of(
                        "mspt", String.format("%.2f", performanceGateStatus.currentAverageMspt()),
                        "threshold", String.format("%.2f", performanceGateStatus.thresholdMspt())
                ));
                MessageUtils.sendMessage(sender, "commands.clearstatus.performance-gate-window", Map.of(
                        "current", String.valueOf(performanceGateStatus.sustainedForSeconds()),
                        "required", String.valueOf(performanceGateStatus.requiredSustainedSeconds())
                ));
                MessageUtils.sendMessage(sender, performanceGateStatus.blockingClears()
                        ? "commands.clearstatus.performance-gate-blocking"
                        : "commands.clearstatus.performance-gate-open");
            }
        }

        MessageUtils.sendMessage(sender, "commands.clearstatus.remaining", Map.of(
                "time", module.getFormattedTimeUntilNextClear()
        ));
        return true;
    }

    @Override
    public String getPermission() {
        return "CLE.clearstatus";
    }

    @Override
    public String getHelpMessageKey() {
        return "commands.help.clearstatus";
    }

    private @NotNull String formatMetric(@NotNull AdaptiveIntervalSettings.Metric metric) {
        return switch (metric) {
            case ENTITY_COUNT -> "ENTITY_COUNT";
            case PLAYER_COUNT -> "PLAYER_COUNT";
        };
    }

    private @NotNull String metricLabel(@NotNull AdaptiveIntervalSettings.Metric metric) {
        return switch (metric) {
            case ENTITY_COUNT -> "entities";
            case PLAYER_COUNT -> "players";
        };
    }
}
