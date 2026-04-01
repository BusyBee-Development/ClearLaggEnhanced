package com.clearlagenhanced.database;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerSqliteIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManagerSqliteIntegrationTest.class.getName());

    @TempDir
    Path tempDir;

    @Test
    void freshStartupCreatesSchema() throws Exception {
        DatabaseSettings settings = sqliteSettings(tempDir, true);

        try (DatabaseManager manager = new DatabaseManager("ClearLaggEnhanced", LOGGER, settings);
             Connection connection = manager.getConnection()) {
            assertTrue(manager.isEnabled());
            assertEquals(DatabaseType.SQLITE, manager.getType());
            assertTrue(tableExists(connection, "clearing_history"));
            assertTrue(tableExists(connection, "performance_data"));
            assertTrue(tableExists(connection, "laggy_chunks"));
            assertEquals(1, rowCount(connection, "schema_history"));
        }

        assertTrue(Files.exists(tempDir.resolve("clearlagg.db")));
    }

    @Test
    void existingLegacyTablesAreAdoptedCleanly() throws Exception {
        Path databaseFile = tempDir.resolve("clearlagg.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE clearing_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    entities_cleared INTEGER NOT NULL,
                    worlds_affected TEXT NOT NULL,
                    clear_type TEXT NOT NULL,
                    duration_ms INTEGER NOT NULL
                )
                """);
        }

        try (DatabaseManager manager = new DatabaseManager("ClearLaggEnhanced", LOGGER, sqliteSettings(tempDir, true));
             Connection connection = manager.getConnection()) {
            assertTrue(tableExists(connection, "schema_history"));
            assertEquals(1, rowCount(connection, "schema_history"));
            assertTrue(tableExists(connection, "performance_data"));
            assertTrue(tableExists(connection, "laggy_chunks"));
        }
    }

    @Test
    void closeAllowsReopenWithoutLeak() throws Exception {
        DatabaseSettings settings = sqliteSettings(tempDir, true);
        DatabaseManager manager = new DatabaseManager("ClearLaggEnhanced", LOGGER, settings);
        manager.close();

        assertThrows(SQLException.class, manager::getConnection);

        try (DatabaseManager reopened = new DatabaseManager("ClearLaggEnhanced", LOGGER, settings);
             Connection connection = reopened.getConnection()) {
            assertTrue(tableExists(connection, "schema_history"));
        }
    }

    @Test
    void disabledDatabaseSkipsInitialization() {
        DatabaseSettings settings = sqliteSettings(tempDir, false);

        try (DatabaseManager manager = new DatabaseManager("ClearLaggEnhanced", LOGGER, settings)) {
            assertFalse(manager.isEnabled());
            assertThrows(SQLException.class, manager::getConnection);
        }

        assertFalse(Files.exists(tempDir.resolve("clearlagg.db")));
    }

    private DatabaseSettings sqliteSettings(Path directory, boolean enabled) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("database.enabled", enabled);
        config.set("database.type", "sqlite");
        config.set("database.file", "clearlagg.db");
        config.set("database.pool.maximum-pool-size", 10);
        config.set("database.pool.minimum-idle", 2);
        config.set("database.pool.connection-timeout-ms", 10_000L);
        config.set("database.pool.idle-timeout-ms", 600_000L);
        config.set("database.pool.max-lifetime-ms", 1_800_000L);
        config.set("database.pool.keepalive-time-ms", 0L);
        config.set("database.pool.validation-timeout-ms", 5_000L);
        return DatabaseSettings.from(config, directory.toFile());
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
