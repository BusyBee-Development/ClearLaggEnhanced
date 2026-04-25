package com.clearlagenhanced.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record ProtectionSettings(
    boolean protectNamed,
    boolean protectTamed,
    boolean protectArmored,
    boolean protectStacked,
    boolean whitelistAllMobs,
    boolean mobsInBoats,
    boolean mobsFromBreeding,
    boolean modernShowcase,
    boolean playerHeads,
    boolean petsModule,
    boolean citizensSupport,
    Set<String> protectedEntityTags,
    Set<String> whitelist,
    Set<String> itemWhitelist
) {
    public static final ProtectionSettings DEFAULTS = new ProtectionSettings(
            true,
            true,
            false,
            false,
            false,
            true,
            true,
            true,
            true,
            true,
            true,
            Set.of(),
            Set.of(),
            Set.of()
    );

    public static @NotNull ProtectionSettings fromConfig(@NotNull ConfigurationSection config) {
        return new ProtectionSettings(
            config.getBoolean("protect-named-entities", true),
            config.getBoolean("protect-tamed-entities", true),
            config.getBoolean("protect-armored-entities", false),
            config.getBoolean("protect-stacked-entities", false),
            config.getBoolean("whitelist-all-mobs", false),
            config.getBoolean("extra-protections.mobs-in-boats", true),
            config.getBoolean("extra-protections.mobs-from-breeding", true),
            config.getBoolean("extra-protections.modern-showcase", true),
            config.getBoolean("extra-protections.player-heads", true),
            config.getBoolean("extra-protections.pets-module", true),
            config.getBoolean("extra-protections.citizens-support", true),
            normalizeExactValues(config.getStringList("extra-protections.protected-entity-tags"), false),
            normalizeExactValues(config.getStringList("whitelist"), true),
            normalizeExactValues(config.getStringList("item-whitelist"), true)
        );
    }

    private static @NotNull Set<String> normalizeExactValues(@NotNull List<String> values, boolean uppercase) {
        if (values.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedValues = new HashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }

            String normalized = value.trim();
            if (normalized.isEmpty()) {
                continue;
            }

            if (uppercase) {
                normalized = normalized.toUpperCase(Locale.ROOT);
            }

            normalizedValues.add(normalized);
        }

        return normalizedValues.isEmpty() ? Set.of() : Set.copyOf(normalizedValues);
    }
}
