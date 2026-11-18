package net.busybee.clearlaggenhanced.modules.integrations.itemsadder;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Zombie;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ItemsAdder 4.0.18 throws from CustomFurniture.byAlreadySpawned for anything that cannot be furniture (#9). */
class ItemsAdderHookTest {

    @Test
    void onlyFurnitureCarriersAreAskedForFurniture() {
        assertTrue(ItemsAdderHook.canBeFurniture(mock(ArmorStand.class)));
        assertTrue(ItemsAdderHook.canBeFurniture(mock(ItemFrame.class)));
        assertTrue(ItemsAdderHook.canBeFurniture(mock(GlowItemFrame.class)));
        assertTrue(ItemsAdderHook.canBeFurniture(mock(ItemDisplay.class)));
    }

    @Test
    void ordinaryMobsAreNeverAskedForFurniture() {
        for (Class<? extends Entity> type : java.util.List.of(Cow.class, Pig.class, Zombie.class)) {
            assertFalse(ItemsAdderHook.canBeFurniture(mock(type)), type.getSimpleName());
        }
    }

    // Only the type matters to canBeFurniture, so the stand-in answers nothing.
    private static <T extends Entity> T mock(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }));
    }
}
