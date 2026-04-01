package com.clearlagenhanced.database.migration;

import com.clearlagenhanced.database.dialect.DatabaseDialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BaselineSchemaMigration implements SchemaMigration {

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "baseline";
    }

    @Override
    public void apply(Connection connection, DatabaseDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS clearing_history (
                    id %s,
                    timestamp TEXT NOT NULL,
                    entities_cleared INTEGER NOT NULL,
                    worlds_affected TEXT NOT NULL,
                    clear_type TEXT NOT NULL,
                    duration_ms INTEGER NOT NULL
                )
                """.formatted(dialect.autoIncrementPrimaryKey()));

            statement.execute("""
                CREATE TABLE IF NOT EXISTS performance_data (
                    id %s,
                    timestamp TEXT NOT NULL,
                    tps REAL NOT NULL,
                    ram_used INTEGER NOT NULL,
                    ram_max INTEGER NOT NULL,
                    entity_count INTEGER NOT NULL
                )
                """.formatted(dialect.autoIncrementPrimaryKey()));

            statement.execute("""
                CREATE TABLE IF NOT EXISTS laggy_chunks (
                    id %s,
                    world VARCHAR(255) NOT NULL,
                    chunk_x INTEGER NOT NULL,
                    chunk_z INTEGER NOT NULL,
                    entity_count INTEGER NOT NULL,
                    last_scanned TEXT NOT NULL
                )
                """.formatted(dialect.autoIncrementPrimaryKey()));
        }

        ensureIndexExists(connection, "laggy_chunks", "idx_laggy_chunks_world",
                "CREATE INDEX idx_laggy_chunks_world ON laggy_chunks(world)");
        ensureIndexExists(connection, "laggy_chunks", "idx_laggy_chunks_entity_count",
                "CREATE INDEX idx_laggy_chunks_entity_count ON laggy_chunks(entity_count DESC)");
        ensureIndexExists(connection, "laggy_chunks", "idx_laggy_chunks_composite",
                "CREATE INDEX idx_laggy_chunks_composite ON laggy_chunks(world, chunk_x, chunk_z)");
    }

    private void ensureIndexExists(Connection connection, String tableName, String indexName, String sql) throws SQLException {
        if (indexExists(connection, tableName, indexName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (resultSet.next()) {
                String existingIndexName = resultSet.getString("INDEX_NAME");
                if (existingIndexName != null && existingIndexName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }

        return false;
    }
}
