package com.clearlagenhanced.database;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSettingsTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesValidSqliteConfig() {
        YamlConfiguration config = baseConfig();
        config.set("database.type", "sqlite");
        config.set("database.file", "clearlagg.db");

        DatabaseSettings settings = DatabaseSettings.from(config, tempDir.toFile());

        assertTrue(settings.enabled());
        assertEquals(DatabaseType.SQLITE, settings.type());
        assertEquals(tempDir.resolve("clearlagg.db").toFile().getAbsolutePath(), settings.sqliteFile().getAbsolutePath());
    }

    @Test
    void parsesValidMysqlConfig() {
        YamlConfiguration config = baseConfig();
        config.set("database.type", "mysql");
        config.set("database.mysql.host", "127.0.0.1");
        config.set("database.mysql.port", 3306);
        config.set("database.mysql.database", "clearlagg");
        config.set("database.mysql.username", "tester");
        config.set("database.mysql.password", "secret");
        config.set("database.mysql.properties.rewriteBatchedStatements", "false");

        DatabaseSettings settings = DatabaseSettings.from(config, tempDir.toFile());

        assertEquals(DatabaseType.MYSQL, settings.type());
        assertEquals("127.0.0.1", settings.mysql().host());
        assertEquals("clearlagg", settings.mysql().database());
        assertEquals("tester", settings.mysql().username());
        assertEquals("false", settings.mysql().properties().get("rewriteBatchedStatements"));
    }

    @Test
    void invalidBackendThrows() {
        YamlConfiguration config = baseConfig();
        config.set("database.type", "postgres");

        assertThrows(DatabaseConfigurationException.class, () -> DatabaseSettings.from(config, tempDir.toFile()));
    }

    @Test
    void missingMysqlHostThrows() {
        YamlConfiguration config = baseConfig();
        config.set("database.type", "mysql");
        config.set("database.mysql.host", " ");

        assertThrows(DatabaseConfigurationException.class, () -> DatabaseSettings.from(config, tempDir.toFile()));
    }

    @Test
    void invalidPoolValuesThrow() {
        YamlConfiguration config = baseConfig();
        config.set("database.pool.maximum-pool-size", 0);

        assertThrows(DatabaseConfigurationException.class, () -> DatabaseSettings.from(config, tempDir.toFile()));
    }

    @Test
    void disabledDatabaseIgnoresInvalidType() {
        YamlConfiguration config = baseConfig();
        config.set("database.enabled", false);
        config.set("database.type", "unknown");

        DatabaseSettings settings = DatabaseSettings.from(config, tempDir.toFile());

        assertEquals(DatabaseType.SQLITE, settings.type());
    }

    private YamlConfiguration baseConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.enabled", true);
        config.set("database.type", "sqlite");
        config.set("database.file", "clearlagg.db");
        config.set("database.mysql.host", "localhost");
        config.set("database.mysql.port", 3306);
        config.set("database.mysql.database", "clearlagg");
        config.set("database.mysql.username", "root");
        config.set("database.mysql.password", "password");
        config.set("database.mysql.ssl-mode", "PREFERRED");
        config.set("database.pool.maximum-pool-size", 10);
        config.set("database.pool.minimum-idle", 2);
        config.set("database.pool.connection-timeout-ms", 10_000L);
        config.set("database.pool.idle-timeout-ms", 600_000L);
        config.set("database.pool.max-lifetime-ms", 1_800_000L);
        config.set("database.pool.keepalive-time-ms", 0L);
        config.set("database.pool.validation-timeout-ms", 5_000L);
        return config;
    }
}
