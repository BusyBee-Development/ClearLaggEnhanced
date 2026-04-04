package com.clearlagenhanced.modules.entityclearing.models;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public record PerformanceGateSettings(boolean enabled, double msptThreshold, int sustainedSeconds) {

    private static final PerformanceGateSettings DISABLED = new PerformanceGateSettings(false, 45.0D, 300);

    public static @NotNull PerformanceGateSettings fromConfig(@Nullable ConfigurationSection section, @NotNull Logger logger) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return DISABLED;
        }

        double msptThreshold = section.getDouble("mspt-threshold", 45.0D);
        if (msptThreshold <= 0.0D) {
            logger.warning("Entity clearing performance gate uses an invalid mspt-threshold. Falling back to disabled state.");
            return DISABLED;
        }

        int sustainedSeconds = section.getInt("sustained-seconds", 300);
        if (sustainedSeconds <= 0) {
            logger.warning("Entity clearing performance gate uses an invalid sustained-seconds value. Falling back to disabled state.");
            return DISABLED;
        }

        return new PerformanceGateSettings(true, msptThreshold, sustainedSeconds);
    }
}
