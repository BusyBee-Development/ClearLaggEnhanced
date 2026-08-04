package net.busybee.clearlaggenhanced.utils;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class ChunkUtils {

    private static Method getChunkAtIfLoadedMethod;
    private static boolean paperApiChecked = false;

    @Nullable
    public static Chunk getChunkAtIfLoaded(@NotNull World world, int x, int z) {
        if (!paperApiChecked) {
            try {
                getChunkAtIfLoadedMethod = World.class.getMethod("getChunkAtIfLoaded", int.class, int.class);
            } catch (NoSuchMethodException ignored) {
            }
            paperApiChecked = true;
        }

        if (getChunkAtIfLoadedMethod != null) {
            try {
                return (Chunk) getChunkAtIfLoadedMethod.invoke(world, x, z);
            } catch (Exception ignored) {
            }
        }

        // Fallback to Spigot API
        if (world.isChunkLoaded(x, z)) {
            try {
                return world.getChunkAt(x, z);
            } catch (Exception e) {
                // This can still happen on Paper even if isChunkLoaded is true
                return null;
            }
        }

        return null;
    }

    @Nullable
    public static Chunk getChunkAtIfLoaded(@NotNull Location loc) {
        World world = loc.getWorld();
        if (world == null) return null;
        return getChunkAtIfLoaded(world, loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }
}
