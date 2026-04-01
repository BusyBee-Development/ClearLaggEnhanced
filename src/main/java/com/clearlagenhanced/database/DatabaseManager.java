package com.clearlagenhanced.database;

<<<<<<< HEAD
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
=======
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
>>>>>>> dev
            initialize();
        }
    }

    private void initialize() {
<<<<<<< HEAD
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

=======
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
>>>>>>> dev
    public void close() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception e) {
<<<<<<< HEAD
                plugin.getLogger().warning("Error closing database pool: " + e.getMessage());
=======
                logger.warning("Error closing database pool: " + e.getMessage());
            } finally {
                dataSource = null;
>>>>>>> dev
            }
        }
    }
}
