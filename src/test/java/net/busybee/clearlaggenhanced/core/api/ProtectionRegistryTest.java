package net.busybee.clearlaggenhanced.core.api;

import net.busybee.clearlaggenhanced.api.EntityProtection;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectionRegistryTest {

    private final AtomicInteger warnings = new AtomicInteger();
    private ProtectionRegistry registry;
    private Plugin owner;
    private Entity entity;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                warnings.incrementAndGet();
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });

        registry = new ProtectionRegistry(logger);
        owner = stub(Plugin.class);
        entity = stub(Entity.class);
    }

    private static <T> T stub(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "getName" -> "Owner";
                    default -> throw new UnsupportedOperationException(method.getName());
                }));
    }

    @Test
    void nothingRegisteredProtectsNothing() {
        assertFalse(registry.isProtected(entity));
    }

    @Test
    void anyRegisteredProtectionKeepsTheEntity() {
        registry.register(owner, e -> false);
        assertFalse(registry.isProtected(entity));

        registry.register(owner, e -> true);
        assertTrue(registry.isProtected(entity));
    }

    @Test
    void sameInstanceIsOnlyRegisteredOnce() {
        AtomicInteger calls = new AtomicInteger();
        EntityProtection protection = e -> {
            calls.incrementAndGet();
            return false;
        };

        registry.register(owner, protection);
        registry.register(owner, protection);
        registry.isProtected(entity);

        assertEquals(1, calls.get());
    }

    @Test
    void unregisterRemovesOnlyThatProtection() {
        EntityProtection protecting = e -> true;
        registry.register(owner, protecting);
        registry.register(owner, e -> false);

        registry.unregister(protecting);

        assertFalse(registry.isProtected(entity));
    }

    @Test
    void unregisterAllRemovesOnlyThatOwnersProtections() {
        Plugin other = stub(Plugin.class);
        registry.register(owner, e -> true);
        registry.unregisterAll(other);
        assertTrue(registry.isProtected(entity));

        registry.unregisterAll(owner);
        assertFalse(registry.isProtected(entity));
    }

    @Test
    void failingProtectionKeepsTheEntityAndWarnsOnce() {
        registry.register(owner, e -> {
            throw new IllegalStateException("broken");
        });

        assertTrue(registry.isProtected(entity));
        assertTrue(registry.isProtected(entity));
        assertEquals(1, warnings.get());
    }
}
