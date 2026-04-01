package com.clearlagenhanced.database.datasource;

import com.clearlagenhanced.database.DatabaseSettings;
import com.clearlagenhanced.database.DatabaseSettings.MySqlSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

public class MySqlDataSourceFactory implements DataSourceFactory {

    @Override
    public HikariDataSource create(String pluginName, DatabaseSettings settings) {
        MySqlSettings mysql = settings.mysql();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://" + mysql.host() + ":" + mysql.port() + "/" + mysql.database());
        hikariConfig.setPoolName(pluginName + "-MySQL");
        hikariConfig.setMaximumPoolSize(settings.pool().maximumPoolSize());
        hikariConfig.setMinimumIdle(settings.pool().minimumIdle());
        hikariConfig.setConnectionTimeout(settings.pool().connectionTimeoutMs());
        hikariConfig.setValidationTimeout(settings.pool().validationTimeoutMs());
        hikariConfig.setIdleTimeout(settings.pool().idleTimeoutMs());
        hikariConfig.setMaxLifetime(settings.pool().maxLifetimeMs());
        if (settings.pool().keepaliveTimeMs() > 0L) {
            hikariConfig.setKeepaliveTime(settings.pool().keepaliveTimeMs());
        }
        hikariConfig.setConnectionTestQuery("SELECT 1");

        hikariConfig.addDataSourceProperty("user", mysql.username());
        hikariConfig.addDataSourceProperty("password", mysql.password());
        hikariConfig.addDataSourceProperty("useUnicode", "true");
        hikariConfig.addDataSourceProperty("characterEncoding", "utf8");
        hikariConfig.addDataSourceProperty("connectionTimeZone", "UTC");
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("tcpKeepAlive", "true");
        hikariConfig.addDataSourceProperty("sslMode", mysql.sslMode());

        for (Map.Entry<String, String> entry : mysql.properties().entrySet()) {
            hikariConfig.addDataSourceProperty(entry.getKey(), entry.getValue());
        }

        return new HikariDataSource(hikariConfig);
    }
}
