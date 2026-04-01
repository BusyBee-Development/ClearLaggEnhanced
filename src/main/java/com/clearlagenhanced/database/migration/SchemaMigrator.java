package com.clearlagenhanced.database.migration;

import com.clearlagenhanced.database.DatabaseInitializationException;
import com.clearlagenhanced.database.dialect.DatabaseDialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class SchemaMigrator {

    private final Logger logger;
    private final DatabaseDialect dialect;
    private final List<SchemaMigration> migrations;

    public SchemaMigrator(Logger logger, DatabaseDialect dialect) {
        this.logger = logger;
        this.dialect = dialect;
        this.migrations = new ArrayList<>();
        this.migrations.add(new BaselineSchemaMigration());
        this.migrations.sort(Comparator.comparingInt(SchemaMigration::version));
    }

    public void migrate(Connection connection) {
        try {
            ensureSchemaHistoryTable(connection);

            Set<Integer> appliedVersions = loadAppliedVersions(connection);
            boolean legacySchema = !appliedVersions.contains(1) && hasLegacyTables(connection);

            for (SchemaMigration migration : migrations) {
                if (appliedVersions.contains(migration.version())) {
                    continue;
                }

                if (legacySchema && migration.version() == 1) {
                    logger.info("Adopting existing " + dialect.name() + " schema as baseline.");
                } else {
                    logger.info("Applying database migration V" + migration.version() + "__" + migration.description() + ".");
                }

                migration.apply(connection, dialect);
                recordMigration(connection, migration, legacySchema && migration.version() == 1);
            }
        } catch (SQLException exception) {
            throw new DatabaseInitializationException("Failed to migrate database schema.", exception);
        }
    }

    private void ensureSchemaHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS schema_history (
                    version INTEGER NOT NULL PRIMARY KEY,
                    description VARCHAR(255) NOT NULL,
                    installed_at VARCHAR(64) NOT NULL
                )
                """);
        }
    }

    private Set<Integer> loadAppliedVersions(Connection connection) throws SQLException {
        Set<Integer> appliedVersions = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version FROM schema_history")) {
            while (resultSet.next()) {
                appliedVersions.add(resultSet.getInt("version"));
            }
        }
        return appliedVersions;
    }

    private boolean hasLegacyTables(Connection connection) throws SQLException {
        return tableExists(connection, "clearing_history")
                || tableExists(connection, "performance_data")
                || tableExists(connection, "laggy_chunks");
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getTables(connection.getCatalog(), null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String existingTableName = resultSet.getString("TABLE_NAME");
                if (existingTableName != null && existingTableName.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void recordMigration(Connection connection, SchemaMigration migration, boolean adopted) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO schema_history(version, description, installed_at) VALUES (?, ?, ?)"
        )) {
            statement.setInt(1, migration.version());
            statement.setString(2, adopted ? migration.description() + " (adopted)" : migration.description());
            statement.setString(3, Instant.now().toString());
            statement.executeUpdate();
        }
    }
}
