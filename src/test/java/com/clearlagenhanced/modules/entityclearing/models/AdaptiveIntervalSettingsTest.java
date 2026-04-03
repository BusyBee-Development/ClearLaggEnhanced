package com.clearlagenhanced.modules.entityclearing.models;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveIntervalSettingsTest {

    private static final Logger LOGGER = Logger.getLogger(AdaptiveIntervalSettingsTest.class.getName());

    @Test
    void parsesAndResolvesEntityCountTiers() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("adaptive-interval.enabled", true);
        config.set("adaptive-interval.metric", "ENTITY_COUNT");
        config.set("adaptive-interval.tiers", java.util.List.of(
                java.util.Map.of("threshold", 0, "interval", 900),
                java.util.Map.of("threshold", 5000, "interval", 750),
                java.util.Map.of("threshold", 7000, "interval", 600)
        ));

        AdaptiveIntervalSettings settings = AdaptiveIntervalSettings.fromConfig(
                config.getConfigurationSection("adaptive-interval"),
                LOGGER
        );

        assertTrue(settings.enabled());
        assertEquals(AdaptiveIntervalSettings.Metric.ENTITY_COUNT, settings.metric());
        assertEquals(900, settings.resolveInterval(2500, 300));
        assertEquals(750, settings.resolveInterval(5000, 300));
        assertEquals(600, settings.resolveInterval(8500, 300));
    }

    @Test
    void invalidMetricDisablesAdaptiveIntervals() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("adaptive-interval.enabled", true);
        config.set("adaptive-interval.metric", "MEMORY");
        config.set("adaptive-interval.tiers", java.util.List.of(
                java.util.Map.of("threshold", 0, "interval", 900)
        ));

        AdaptiveIntervalSettings settings = AdaptiveIntervalSettings.fromConfig(
                config.getConfigurationSection("adaptive-interval"),
                LOGGER
        );

        assertFalse(settings.enabled());
        assertEquals(300, settings.resolveInterval(1000, 300));
    }

    @Test
    void highestThresholdWinsAfterTierNormalization() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("adaptive-interval.enabled", true);
        config.set("adaptive-interval.metric", "PLAYERS");
        config.set("adaptive-interval.tiers", java.util.List.of(
                java.util.Map.of("threshold", 0, "interval", 900),
                java.util.Map.of("threshold", 20, "interval", 600),
                java.util.Map.of("threshold", 20, "interval", 540)
        ));

        AdaptiveIntervalSettings settings = AdaptiveIntervalSettings.fromConfig(
                config.getConfigurationSection("adaptive-interval"),
                LOGGER
        );

        assertTrue(settings.enabled());
        assertEquals(540, settings.resolveInterval(20, 300));
        assertEquals(540, settings.resolveInterval(35, 300));
    }
}
