package com.clearlagenhanced.database.datasource;

import com.clearlagenhanced.database.DatabaseInitializationException;
import com.clearlagenhanced.database.DatabaseSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.sqlite.SQLiteConfig;

import java.io.File;

public class SqliteDataSourceFactory implements DataSourceFactory {

    @Override
    public HikariDataSource create(String pluginName, DatabaseSettings settings) {
        File sqliteFile = settings.sqliteFile();
        File parentFile = sqliteFile.getParentFile();
        if (parentFile != null && !parentFile.exists() && !parentFile.mkdirs()) {
            throw new DatabaseInitializationException(
                    "Failed to create SQLite directory: " + parentFile.getAbsolutePath(),
                    null
            );
        }

        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.enforceForeignKeys(true);
        sqliteConfig.setBusyTimeout((int) Math.min(Integer.MAX_VALUE, settings.pool().connectionTimeoutMs()));
        sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        sqliteConfig.setTempStore(SQLiteConfig.TempStore.MEMORY);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
        hikariConfig.setPoolName(pluginName + "-SQLite");
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setMinimumIdle(Math.min(Math.max(settings.pool().minimumIdle(), 0), 1));
        hikariConfig.setConnectionTimeout(settings.pool().connectionTimeoutMs());
        hikariConfig.setValidationTimeout(settings.pool().validationTimeoutMs());
        hikariConfig.setIdleTimeout(settings.pool().idleTimeoutMs());
        hikariConfig.setMaxLifetime(settings.pool().maxLifetimeMs());
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setDataSourceProperties(sqliteConfig.toProperties());

        return new HikariDataSource(hikariConfig);
    }
}
