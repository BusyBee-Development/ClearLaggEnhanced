package net.busybee.clearlaggenhanced.models;

import net.busybee.clearlaggenhanced.utils.EntityTypeNames;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
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
    boolean protectPassiveMobs,
    boolean mobsInBoats,
    boolean mobsInMinecarts,
    boolean leashedMobs,
    boolean saddledMobs,
    boolean riddenMobs,
    boolean bucketMobs,
    boolean mobsFromBreeding,
    boolean modernShowcase,
    boolean playerHeads,
    boolean petsModule,
    boolean citizensSupport,
    boolean mythicMobs,
    boolean infernalMobs,
    boolean griefPrevention3D,
    boolean oraxen,
    boolean nexo,
    boolean itemsAdder,
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
        Set<String> entityEntries = normalizeExactValues(mainConfig.getStringList("whitelist"), true);
        Set<String> itemEntries = normalizeExactValues(mainConfig.getStringList("item-whitelist"), true);

        return new ProtectionSettings(
            mainConfig.getBoolean("protect-named-entities", true),
            mainConfig.getBoolean("protect-persistent-named-only", false),
            mainConfig.getBoolean("protect-tamed-entities", true),
            mainConfig.getBoolean("protect-armored-entities", false),
            mainConfig.getBoolean("protect-stacked-entities", false),
            mainConfig.getBoolean("whitelist-all-mobs", false),
            mainConfig.getBoolean("protect-passive-mobs", false),
            mainConfig.getBoolean("extra-protections.mobs-in-boats", true),
            mainConfig.getBoolean("extra-protections.mobs-in-minecarts", true),
            mainConfig.getBoolean("extra-protections.leashed-mobs", true),
            mainConfig.getBoolean("extra-protections.saddled-mobs", true),
            mainConfig.getBoolean("extra-protections.ridden-mobs", true),
            mainConfig.getBoolean("extra-protections.bucket-mobs", true),
            mainConfig.getBoolean("extra-protections.mobs-from-breeding", true),
            mainConfig.getBoolean("extra-protections.modern-showcase", true),
            mainConfig.getBoolean("extra-protections.player-heads", true),
            mainConfig.getBoolean("extra-protections.pets-module", true),
            mainConfig.getBoolean("extra-protections.citizens-support", true),
            mainConfig.getBoolean("extra-protections.mythic-mobs", true),
            mainConfig.getBoolean("extra-protections.infernal-mobs", true),
            mainConfig.getBoolean("extra-protections.grief-prevention-3d", true),
            mainConfig.getBoolean("extra-protections.oraxen", true),
            mainConfig.getBoolean("extra-protections.nexo", true),
            mainConfig.getBoolean("extra-protections.items-adder", true),
            normalizeExactValues(mainConfig.getStringList("extra-protections.protected-entity-tags"), false),
            resolveEntityWhitelist(entityEntries, itemEntries),
            resolveItemWhitelist(entityEntries, itemEntries)
        );
    }

    // Entity type names to protect: every whitelist entry under all the names it has across server
    // versions, plus item-whitelist entries that can only mean an entity (not a material, or a
    // non-mob entity such as ARMOR_STAND that shares its name with its item).
    private static @NotNull Set<String> resolveEntityWhitelist(@NotNull Set<String> entityEntries, @NotNull Set<String> itemEntries) {
        Set<String> resolved = new HashSet<>();
        for (String entry : entityEntries) {
            resolved.addAll(EntityTypeNames.expand(entry));
        }

        for (String entry : itemEntries) {
            EntityType type = EntityTypeNames.resolve(entry);
            if (type != null && (!EntityTypeNames.isMob(type) || Material.getMaterial(entry) == null)) {
                resolved.addAll(EntityTypeNames.expand(entry));
            }
        }

        return Set.copyOf(resolved);
    }

    // Material names to protect as dropped items: the item-whitelist, plus whitelist entries that
    // name a material and no mob (TRIDENT, ARMOR_STAND, MINECART, ...), so whitelisting one of those
    // in either list keeps both the placed entity and its dropped item.
    private static @NotNull Set<String> resolveItemWhitelist(@NotNull Set<String> entityEntries, @NotNull Set<String> itemEntries) {
        Set<String> resolved = new HashSet<>(itemEntries);
        for (String entry : entityEntries) {
            if (Material.getMaterial(entry) == null) {
                continue;
            }

            EntityType type = EntityTypeNames.resolve(entry);
            if (type == null || !EntityTypeNames.isMob(type)) {
                resolved.add(entry);
            }
        }

        return Set.copyOf(resolved);
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
