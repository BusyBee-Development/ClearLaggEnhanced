package com.clearlagenhanced.database;

import com.clearlagenhanced.ClearLaggEnhanced;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<Void> logClearingHistory(int entitiesCleared, List<String> worlds,
                                                      String clearType, long durationMs) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO clearing_history (timestamp, entities_cleared, worlds_affected, clear_type, duration_ms)
                VALUES (?, ?, ?, ?, ?)
            """;

            try (Connection conn = dataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, LocalDateTime.now().toString());
                stmt.setInt(2, entitiesCleared);
                stmt.setString(3, String.join(",", worlds));
                stmt.setString(4, clearType);
                stmt.setLong(5, durationMs);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to log clearing history: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> logPerformanceData(double tps, long ramUsed, long ramMax, int entityCount) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO performance_data (timestamp, tps, ram_used, ram_max, entity_count)
                VALUES (?, ?, ?, ?, ?)
            """;

            try (Connection conn = dataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, LocalDateTime.now().toString());
                stmt.setDouble(2, tps);
                stmt.setLong(3, ramUsed);
                stmt.setLong(4, ramMax);
                stmt.setInt(5, entityCount);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to log performance data: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<List<LaggyChunkData>> getLaggyChunks(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LaggyChunkData> chunks = new ArrayList<>();
            String sql = "SELECT world, chunk_x, chunk_z, entity_count FROM laggy_chunks ORDER BY entity_count DESC LIMIT ?";

            try (Connection conn = dataSource().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        chunks.add(new LaggyChunkData(
                                rs.getString("world"),
                                rs.getInt("chunk_x"),
                                rs.getInt("chunk_z"),
                                rs.getInt("entity_count")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get laggy chunks: " + e.getMessage());
            }

            return chunks;
        });
    }

    public CompletableFuture<Void> updateLaggyChunks(List<LaggyChunkData> chunks) {
        return CompletableFuture.runAsync(() -> {
            String deleteSql = "DELETE FROM laggy_chunks";
            String insertSql = """
                    INSERT INTO laggy_chunks (world, chunk_x, chunk_z, entity_count, last_scanned)
                    VALUES (?, ?, ?, ?, ?)
                """;
            try (Connection conn = dataSource().getConnection()) {
                boolean oldAuto = conn.getAutoCommit();
                conn.setAutoCommit(false);
                try (Statement del = conn.createStatement()) {
                    del.execute(deleteSql);
                }
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    for (LaggyChunkData chunk : chunks) {
                        stmt.setString(1, chunk.world());
                        stmt.setInt(2, chunk.chunkX());
                        stmt.setInt(3, chunk.chunkZ());
                        stmt.setInt(4, chunk.entityCount());
                        stmt.setString(5, LocalDateTime.now().toString());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
                conn.commit();
                conn.setAutoCommit(oldAuto);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to update laggy chunks: " + e.getMessage());
            }
        });
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

    public record LaggyChunkData(String world, int chunkX, int chunkZ, int entityCount) {
    }
}
