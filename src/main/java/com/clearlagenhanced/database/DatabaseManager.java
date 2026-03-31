package com.clearlagenhanced.database;

import com.clearlagenhanced.database.datasource.DataSourceFactory;
import com.clearlagenhanced.database.datasource.MySqlDataSourceFactory;
import com.clearlagenhanced.database.datasource.SqliteDataSourceFactory;
import com.clearlagenhanced.database.dialect.DatabaseDialect;
import com.clearlagenhanced.database.dialect.MySqlDialect;
import com.clearlagenhanced.database.dialect.SqliteDialect;
import com.clearlagenhanced.database.migration.SchemaMigrator;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseManager implements AutoCloseable {

    private final String pluginName;
    private final Logger logger;
    private final DatabaseSettings settings;
    private final DatabaseType type;
    private HikariDataSource dataSource;

    public DatabaseManager(String pluginName, Logger logger, DatabaseSettings settings) {
        this.pluginName = pluginName;
        this.logger = logger;
        this.settings = settings;
        this.type = settings.type();

        if (settings.enabled()) {
            initialize();
        }
    }

    private void initialize() {
        DataSourceFactory factory = createFactory(type);
        DatabaseDialect dialect = createDialect(type);

        this.dataSource = factory.create(pluginName, settings);

        try (Connection connection = getConnection()) {
            new SchemaMigrator(logger, dialect).migrate(connection);
        } catch (SQLException exception) {
            close();
            throw new DatabaseInitializationException(
                    "Failed to initialize " + type.getDisplayName() + " database at " + settings.describeTarget() + ".",
                    exception
            );
        } catch (RuntimeException exception) {
            close();
            throw exception;
        }

        logger.info("Database initialized successfully: backend="
                + type.getConfigValue()
                + ", target="
                + settings.describeTarget()
                + ", pool="
                + dataSource.getPoolName());
    }

    private DataSourceFactory createFactory(DatabaseType databaseType) {
        return switch (databaseType) {
            case SQLITE -> new SqliteDataSourceFactory();
            case MYSQL -> new MySqlDataSourceFactory();
        };
    }

    private DatabaseDialect createDialect(DatabaseType databaseType) {
        return switch (databaseType) {
            case SQLITE -> new SqliteDialect();
            case MYSQL -> new MySqlDialect();
        };
    }

    public boolean isEnabled() {
        return settings.enabled() && dataSource != null;
    }

    public DatabaseType getType() {
        return type;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database is not enabled.");
        }

        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception e) {
                logger.warning("Error closing database pool: " + e.getMessage());
            } finally {
                dataSource = null;
            }
        }
    }
}
