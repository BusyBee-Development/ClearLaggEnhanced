package net.busybee.clearlaggenhanced.models;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record ProtectionSettings(
    boolean protectNamed,
    boolean protectPersistentNamedOnly,
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
    boolean mythicMobs,
    boolean infernalMobs,
    boolean modDex,
    Set<String> protectedEntityTags,
    Set<String> whitelist,
    Set<String> itemWhitelist
) {
    public static final ProtectionSettings DEFAULTS = new ProtectionSettings(
            true,
            false,
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
            true,
            true,
            true,
            Set.of(),
            Set.of(),
            Set.of()
    );

    public static @NotNull ProtectionSettings fromConfig(@NotNull ConfigurationSection mainConfig, @NotNull ConfigurationSection entitiesConfig) {
        return new ProtectionSettings(
            mainConfig.getBoolean("protect-named-entities", true),
            mainConfig.getBoolean("protect-persistent-named-only", false),
            mainConfig.getBoolean("protect-tamed-entities", true),
            mainConfig.getBoolean("protect-armored-entities", false),
            mainConfig.getBoolean("protect-stacked-entities", false),
            mainConfig.getBoolean("whitelist-all-mobs", false),
            mainConfig.getBoolean("extra-protections.mobs-in-boats", true),
            mainConfig.getBoolean("extra-protections.mobs-from-breeding", true),
            mainConfig.getBoolean("extra-protections.modern-showcase", true),
            mainConfig.getBoolean("extra-protections.player-heads", true),
            mainConfig.getBoolean("extra-protections.pets-module", true),
            mainConfig.getBoolean("extra-protections.citizens-support", true),
            mainConfig.getBoolean("extra-protections.mythic-mobs", true),
            mainConfig.getBoolean("extra-protections.infernal-mobs", true),
            mainConfig.getBoolean("extra-protections.mod-dex", true),
            normalizeExactValues(mainConfig.getStringList("extra-protections.protected-entity-tags"), false),
            normalizeExactValues(mainConfig.getStringList("whitelist"), true),
            normalizeExactValues(mainConfig.getStringList("item-whitelist"), true)
        );
    }

    public static @NotNull ProtectionSettings fromConfig(@NotNull ConfigurationSection config) {
        return fromConfig(config, config);
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
