package com.clearlagenhanced.utils;

import org.bukkit.configuration.ConfigurationSection;
import java.util.Set;
import java.util.stream.Collectors;

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
    Set<String> whitelist,
    Set<String> itemWhitelist
) {
    public static ProtectionSettings fromConfig(ConfigurationSection config) {
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
            config.getStringList("whitelist").stream().map(String::toUpperCase).collect(Collectors.toSet()),
            config.getStringList("item-whitelist").stream().map(String::toUpperCase).collect(Collectors.toSet())
        );
    }
}
