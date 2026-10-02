package net.busybee.clearlaggenhanced.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class EntityTypeNames {

    // EntityType names before 1.20.5 -> the names used since.
    private static final Map<String, String> RENAMED = Map.ofEntries(
            Map.entry("DROPPED_ITEM", "ITEM"),
            Map.entry("LEASH_HITCH", "LEASH_KNOT"),
            Map.entry("ENDER_SIGNAL", "EYE_OF_ENDER"),
            Map.entry("SPLASH_POTION", "POTION"),
            Map.entry("THROWN_EXP_BOTTLE", "EXPERIENCE_BOTTLE"),
            Map.entry("PRIMED_TNT", "TNT"),
            Map.entry("FIREWORK", "FIREWORK_ROCKET"),
            Map.entry("MINECART_COMMAND", "COMMAND_BLOCK_MINECART"),
            Map.entry("MINECART_CHEST", "CHEST_MINECART"),
            Map.entry("MINECART_FURNACE", "FURNACE_MINECART"),
            Map.entry("MINECART_TNT", "TNT_MINECART"),
            Map.entry("MINECART_HOPPER", "HOPPER_MINECART"),
            Map.entry("MINECART_MOB_SPAWNER", "SPAWNER_MINECART"),
            Map.entry("ENDER_CRYSTAL", "END_CRYSTAL"),
            Map.entry("FISHING_HOOK", "FISHING_BOBBER"),
            Map.entry("LIGHTNING", "LIGHTNING_BOLT"),
            Map.entry("MUSHROOM_COW", "MOOSHROOM"),
            Map.entry("SNOWMAN", "SNOW_GOLEM")
    );
    private static final Map<String, String> RENAMED_FROM = invert(RENAMED);

    private EntityTypeNames() {
    }

    // Every name the same entity goes by across server versions, the given one included, so a
    // whitelist entry written for one version still matches on another.
    public static @NotNull Set<String> expand(@NotNull String name) {
        Set<String> names = new LinkedHashSet<>();
        names.add(name);

        String newName = RENAMED.get(name);
        if (newName != null) names.add(newName);

        String oldName = RENAMED_FROM.get(name);
        if (oldName != null) names.add(oldName);

        // 1.21.2 split BOAT and CHEST_BOAT into one type per wood.
        if (name.equals("BOAT") || name.equals("CHEST_BOAT")) {
            boolean chest = name.equals("CHEST_BOAT");
            for (EntityType type : EntityType.values()) {
                if (isBoatVariant(type.name()) && isChestBoatVariant(type.name()) == chest) {
                    names.add(type.name());
                }
            }
        } else if (isBoatVariant(name)) {
            names.add(isChestBoatVariant(name) ? "CHEST_BOAT" : "BOAT");
        }

        return names;
    }

    // The entity type a config entry refers to on this server version, or null if there is none.
    public static @Nullable EntityType resolve(@NotNull String name) {
        for (String candidate : expand(name)) {
            try {
                EntityType type = EntityType.valueOf(candidate);
                if (type != EntityType.UNKNOWN) return type;
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    private static boolean isBoatVariant(@NotNull String name) {
        return name.endsWith("_BOAT") || name.endsWith("_RAFT");
    }

    private static boolean isChestBoatVariant(@NotNull String name) {
        return name.endsWith("CHEST_BOAT") || name.endsWith("CHEST_RAFT");
    }

    // Armor stands are living entities to Bukkit, so "is a mob" is the test for an actual creature.
    public static boolean isMob(@NotNull EntityType type) {
        Class<? extends Entity> entityClass = type.getEntityClass();
        return entityClass != null && Mob.class.isAssignableFrom(entityClass);
    }

    private static @NotNull Map<String, String> invert(@NotNull Map<String, String> map) {
        Map<String, String> inverted = new HashMap<>();
        map.forEach((key, value) -> inverted.put(value, key));
        return Map.copyOf(inverted);
    }
}
