package com.clearlagenhanced.database;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;

public class DatabaseManager {

    private final ClearLaggEnhanced plugin;
    private final String databasePath;
    private HikariDataSource dataSource;

    public DatabaseManager(ClearLaggEnhanced plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder() + File.separator +
                plugin.getConfigManager().getString("database.file", "data.db");

        if (plugin.getConfigManager().getBoolean("database.enabled", true)) {
            initialize();
        }
    }

    private void initialize() {
        try {
            setupPool();
            createTables();
            plugin.getLogger().info("Database initialized successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    private void setupPool() {
        HikariConfig hc = new HikariConfig();
        String url = "jdbc:sqlite:" + databasePath;
        hc.setJdbcUrl(url);
        hc.setDriverClassName("org.sqlite.JDBC");
        hc.setMaximumPoolSize(5);
        hc.setMinimumIdle(1);
        hc.setPoolName(plugin.getName() + "-Hikari");
        hc.setConnectionTimeout(10_000);
        hc.setIdleTimeout(60_000);
        hc.setMaxLifetime(30 * 60_000);
        this.dataSource = new HikariDataSource(hc);
    }

    private DataSource dataSource() {
        return dataSource;
    }

    private void createTables() throws SQLException {
        String clearingHistoryTable = """
            CREATE TABLE IF NOT EXISTS clearing_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                entities_cleared INTEGER NOT NULL,
                worlds_affected TEXT NOT NULL,
                clear_type TEXT NOT NULL,
                duration_ms INTEGER NOT NULL
            )
        """;

        String performanceDataTable = """
            CREATE TABLE IF NOT EXISTS performance_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                tps REAL NOT NULL,
                ram_used INTEGER NOT NULL,
                ram_max INTEGER NOT NULL,
                entity_count INTEGER NOT NULL
            )
        """;

        String laggyChunksTable = """
            CREATE TABLE IF NOT EXISTS laggy_chunks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                world VARCHAR(255) NOT NULL,
                chunk_x INTEGER NOT NULL,
                chunk_z INTEGER NOT NULL,
                entity_count INTEGER NOT NULL,
                last_scanned TEXT NOT NULL
            )
        """;

        try (Connection conn = dataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(clearingHistoryTable);
            stmt.execute(performanceDataTable);
            stmt.execute(laggyChunksTable);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_laggy_chunks_world ON laggy_chunks(world)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_laggy_chunks_entity_count ON laggy_chunks(entity_count DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_laggy_chunks_composite ON laggy_chunks(world, chunk_x, chunk_z)");
        }
    }

    public void close() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception e) {
                plugin.getLogger().warning("Error closing database pool: " + e.getMessage());
            }
        }
    }
}
