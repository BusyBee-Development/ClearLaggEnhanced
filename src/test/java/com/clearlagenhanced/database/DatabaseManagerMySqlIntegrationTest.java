package com.clearlagenhanced.database;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class DatabaseManagerMySqlIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManagerMySqlIntegrationTest.class.getName());

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("clearlagg")
            .withUsername("tester")
            .withPassword("secret");

    @TempDir
    Path tempDir;

    @BeforeEach
    void cleanSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS schema_history");
            statement.execute("DROP TABLE IF EXISTS laggy_chunks");
            statement.execute("DROP TABLE IF EXISTS performance_data");
            statement.execute("DROP TABLE IF EXISTS clearing_history");
        }
    }

    @Test
    void freshStartupCreatesSchema() throws Exception {
        try (DatabaseManager manager = new DatabaseManager("ClearLaggEnhanced", LOGGER, mysqlSettings(MYSQL.getPassword()));
             Connection connection = manager.getConnection()) {
            assertTrue(manager.isEnabled());
            assertEquals(DatabaseType.MYSQL, manager.getType());
            assertTrue(tableExists(connection, "clearing_history"));
            assertTrue(tableExists(connection, "performance_data"));
            assertTrue(tableExists(connection, "laggy_chunks"));
            assertEquals(1, rowCount(connection, "schema_history"));
        }
    }

    @Test
    void existingLegacySchemaIsAdoptedCleanly() throws Exception {
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE clearing_history (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    timestamp TEXT NOT NULL,
                    entities_cleared INTEGER NOT NULL,
                    worlds_affected TEXT NOT NULL,
                    clear_type TEXT NOT NULL,
                    duration_ms INTEGER NOT NULL
                )
                """);
        }

        try (DatabaseManager manager = new DatabaseManager("ClearLaggEnhanced", LOGGER, mysqlSettings(MYSQL.getPassword()));
             Connection connection = manager.getConnection()) {
            assertEquals(1, rowCount(connection, "schema_history"));
            assertTrue(tableExists(connection, "performance_data"));
            assertTrue(tableExists(connection, "laggy_chunks"));
        }
    }

    @Test
    void invalidCredentialsFailInitialization() {
        assertThrows(DatabaseInitializationException.class, () ->
                new DatabaseManager("ClearLaggEnhanced", LOGGER, mysqlSettings("wrong-password"))
        );
    }

    @Test
    void backendSwitchDoesNotMigrateData() throws Exception {
        DatabaseSettings sqliteSettings = sqliteSettings();
        try (DatabaseManager sqliteManager = new DatabaseManager("ClearLaggEnhanced", LOGGER, sqliteSettings);
             Connection connection = sqliteManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO clearing_history(timestamp, entities_cleared, worlds_affected, clear_type, duration_ms)
                VALUES ('2026-03-31T00:00:00Z', 5, 'world', 'manual', 42)
                """);
            assertEquals(1, rowCount(connection, "clearing_history"));
        }

        try (DatabaseManager mysqlManager = new DatabaseManager("ClearLaggEnhanced", LOGGER, mysqlSettings(MYSQL.getPassword()));
             Connection connection = mysqlManager.getConnection()) {
            assertEquals(0, rowCount(connection, "clearing_history"));
        }
    }

    private DatabaseSettings mysqlSettings(String password) {
        YamlConfiguration config = basePoolConfig();
        config.set("database.enabled", true);
        config.set("database.type", "mysql");
        config.set("database.mysql.host", MYSQL.getHost());
        config.set("database.mysql.port", MYSQL.getMappedPort(3306));
        config.set("database.mysql.database", MYSQL.getDatabaseName());
        config.set("database.mysql.username", MYSQL.getUsername());
        config.set("database.mysql.password", password);
        config.set("database.mysql.ssl-mode", "DISABLED");
        return DatabaseSettings.from(config, tempDir.toFile());
    }

    private DatabaseSettings sqliteSettings() {
        YamlConfiguration config = basePoolConfig();
        config.set("database.enabled", true);
        config.set("database.type", "sqlite");
        config.set("database.file", "clearlagg.db");
        return DatabaseSettings.from(config, tempDir.toFile());
    }

    private YamlConfiguration basePoolConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.pool.maximum-pool-size", 10);
        config.set("database.pool.minimum-idle", 2);
        config.set("database.pool.connection-timeout-ms", 10_000L);
        config.set("database.pool.idle-timeout-ms", 600_000L);
        config.set("database.pool.max-lifetime-ms", 1_800_000L);
        config.set("database.pool.keepalive-time-ms", 0L);
        config.set("database.pool.validation-timeout-ms", 5_000L);
        return config;
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String existing = resultSet.getString("TABLE_NAME");
                if (existing != null && existing.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private int rowCount(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
