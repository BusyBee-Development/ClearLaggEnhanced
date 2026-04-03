package com.clearlagenhanced.modules.entityclearing.models;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public record AdaptiveIntervalSettings(boolean enabled, @NotNull Metric metric, @NotNull List<Tier> tiers) {

    private static final AdaptiveIntervalSettings DISABLED =
            new AdaptiveIntervalSettings(false, Metric.ENTITY_COUNT, List.of());

    public static @NotNull AdaptiveIntervalSettings fromConfig(@Nullable ConfigurationSection section, @NotNull Logger logger) {
        if (section == null || !section.getBoolean("enabled", false)) {
            return DISABLED;
        }

        Metric metric = Metric.fromName(section.getString("metric"));
        if (metric == null) {
            logger.warning("Entity clearing adaptive interval uses an invalid metric. Valid values: ENTITY_COUNT, PLAYER_COUNT. Falling back to the base interval.");
            return DISABLED;
        }

        List<Map<?, ?>> configuredTiers = section.getMapList("tiers");
        if (configuredTiers.isEmpty()) {
            logger.warning("Entity clearing adaptive interval is enabled but no valid tiers are configured. Falling back to the base interval.");
            return DISABLED;
        }

        Map<Integer, Tier> normalizedTiers = new LinkedHashMap<>();
        for (Map<?, ?> tierMap : configuredTiers) {
            Integer threshold = parseInteger(tierMap.get("threshold"));
            Integer interval = parseInteger(tierMap.get("interval"));

            if (threshold == null || threshold < 0) {
                logger.warning("Skipping adaptive interval tier with invalid threshold: " + tierMap);
                continue;
            }

            if (interval == null || interval <= 0) {
                logger.warning("Skipping adaptive interval tier with invalid interval: " + tierMap);
                continue;
            }

            normalizedTiers.put(threshold, new Tier(threshold, interval));
        }

        if (normalizedTiers.isEmpty()) {
            logger.warning("Entity clearing adaptive interval has no usable tiers after validation. Falling back to the base interval.");
            return DISABLED;
        }

        List<Tier> tiers = new ArrayList<>(normalizedTiers.values());
        tiers.sort(Comparator.comparingInt(Tier::threshold));
        return new AdaptiveIntervalSettings(true, metric, List.copyOf(tiers));
    }

    public int resolveInterval(int currentValue, int fallbackInterval) {
        if (!enabled || tiers.isEmpty()) {
            return fallbackInterval;
        }

        int resolvedInterval = fallbackInterval;
        for (Tier tier : tiers) {
            if (currentValue < tier.threshold()) {
                break;
            }

            resolvedInterval = tier.interval();
        }

        return resolvedInterval;
    }

    private static @Nullable Integer parseInteger(@Nullable Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.intValue();
        }

        if (rawValue instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    public enum Metric {
        ENTITY_COUNT,
        PLAYER_COUNT;

        public static @Nullable Metric fromName(@Nullable String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return ENTITY_COUNT;
            }

            return switch (rawValue.trim().toUpperCase(Locale.ROOT)) {
                case "ENTITY_COUNT", "ENTITIES" -> ENTITY_COUNT;
                case "PLAYER_COUNT", "PLAYERS" -> PLAYER_COUNT;
                default -> null;
            };
        }
    }

    public record Tier(int threshold, int interval) {
    }
}
