package net.busybee.clearlaggenhanced.core.updater;

import org.bukkit.plugin.Plugin;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.logging.Logger;

/** A stand-in plugin that serves the real packaged files, plus one made-up default at {@link #TEST_PATH}. */
final class TestPlugin {

    static final String TEST_PATH = "test/config.yml";

    private TestPlugin() {
    }

    static Plugin create(Path dataFolder, Supplier<String> testDefaults) {
        // Every test rewrites real config files, so a live logger floods the build output.
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDataFolder" -> dataFolder.toFile();
                    case "getLogger" -> logger;
                    case "getResource" -> args[0].equals(TEST_PATH)
                            ? new ByteArrayInputStream(testDefaults.get().getBytes(StandardCharsets.UTF_8))
                            : TestPlugin.class.getResourceAsStream("/" + args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
