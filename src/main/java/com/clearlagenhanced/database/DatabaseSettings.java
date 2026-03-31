package com.clearlagenhanced.database;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DatabaseSettings {

    private final boolean enabled;
    private final DatabaseType type;
    private final File sqliteFile;
    private final MySqlSettings mysql;
    private final PoolSettings pool;

    private DatabaseSettings(
            boolean enabled,
            @NotNull DatabaseType type,
            @NotNull File sqliteFile,
            @NotNull MySqlSettings mysql,
            @NotNull PoolSettings pool
    ) {
        this.enabled = enabled;
        this.type = Objects.requireNonNull(type, "type");
        this.sqliteFile = Objects.requireNonNull(sqliteFile, "sqliteFile");
        this.mysql = Objects.requireNonNull(mysql, "mysql");
        this.pool = Objects.requireNonNull(pool, "pool");
    }

    public static DatabaseSettings from(@NotNull FileConfiguration config, @NotNull File dataFolder) {
        boolean enabled = config.getBoolean("database.enabled", true);
        String rawType = config.getString("database.type", DatabaseType.SQLITE.getConfigValue());
        DatabaseType type = enabled
                ? DatabaseType.fromConfigValue(rawType)
                : DatabaseType.fromConfigValueOrDefault(rawType, DatabaseType.SQLITE);

        String filePath = trimToNull(config.getString("database.file", "clearlagg.db"));
        if (filePath == null) {
            filePath = "clearlagg.db";
        }

        File sqliteFile = new File(filePath);
        if (!sqliteFile.isAbsolute()) {
            sqliteFile = new File(dataFolder, filePath);
        }
        sqliteFile = sqliteFile.getAbsoluteFile();

        MySqlSettings mysql = parseMySqlSettings(config);
        PoolSettings pool = parsePoolSettings(config);

        if (enabled) {
            validate(type, sqliteFile, mysql, pool);
        }

        return new DatabaseSettings(enabled, type, sqliteFile, mysql, pool);
    }

    private static MySqlSettings parseMySqlSettings(FileConfiguration config) {
        String host = trimToNull(config.getString("database.mysql.host", "localhost"));
        int port = config.getInt("database.mysql.port", 3306);
        String database = trimToNull(config.getString("database.mysql.database", "clearlagg"));
        String username = trimToNull(config.getString("database.mysql.username", "root"));
        String password = config.getString("database.mysql.password", "");
        String sslMode = trimToNull(config.getString("database.mysql.ssl-mode", "PREFERRED"));

        Map<String, String> properties = new LinkedHashMap<>();
        ConfigurationSection propertiesSection = config.getConfigurationSection("database.mysql.properties");
        if (propertiesSection != null) {
            for (String key : propertiesSection.getKeys(false)) {
                Object value = propertiesSection.get(key);
                if (value != null) {
                    properties.put(key, String.valueOf(value));
                }
            }
        }

        return new MySqlSettings(
                host,
                port,
                database,
                username,
                password == null ? "" : password,
                sslMode == null ? "PREFERRED" : sslMode,
                properties
        );
    }

    private static PoolSettings parsePoolSettings(FileConfiguration config) {
        return new PoolSettings(
                config.getInt("database.pool.maximum-pool-size", 10),
                config.getInt("database.pool.minimum-idle", 2),
                config.getLong("database.pool.connection-timeout-ms", 10_000L),
                config.getLong("database.pool.idle-timeout-ms", 600_000L),
                config.getLong("database.pool.max-lifetime-ms", 1_800_000L),
                config.getLong("database.pool.keepalive-time-ms", 0L),
                config.getLong("database.pool.validation-timeout-ms", 5_000L)
        );
    }

    private static void validate(DatabaseType type, File sqliteFile, MySqlSettings mysql, PoolSettings pool) {
        if (type == DatabaseType.SQLITE && trimToNull(sqliteFile.getPath()) == null) {
            throw new DatabaseConfigurationException("database.file must not be blank when database.type=sqlite.");
        }

        if (type == DatabaseType.MYSQL) {
            requireNotBlank(mysql.host(), "database.mysql.host");
            requireNotBlank(mysql.database(), "database.mysql.database");
            requireNotBlank(mysql.username(), "database.mysql.username");

            if (mysql.port() <= 0 || mysql.port() > 65_535) {
                throw new DatabaseConfigurationException("database.mysql.port must be between 1 and 65535.");
            }
        }

        if (pool.maximumPoolSize() <= 0) {
            throw new DatabaseConfigurationException("database.pool.maximum-pool-size must be greater than 0.");
        }
        if (pool.minimumIdle() < 0) {
            throw new DatabaseConfigurationException("database.pool.minimum-idle must be 0 or greater.");
        }
        if (pool.connectionTimeoutMs() <= 0L) {
            throw new DatabaseConfigurationException("database.pool.connection-timeout-ms must be greater than 0.");
        }
        if (pool.idleTimeoutMs() <= 0L) {
            throw new DatabaseConfigurationException("database.pool.idle-timeout-ms must be greater than 0.");
        }
        if (pool.maxLifetimeMs() <= 0L) {
            throw new DatabaseConfigurationException("database.pool.max-lifetime-ms must be greater than 0.");
        }
        if (pool.keepaliveTimeMs() < 0L) {
            throw new DatabaseConfigurationException("database.pool.keepalive-time-ms must be 0 or greater.");
        }
        if (pool.validationTimeoutMs() <= 0L) {
            throw new DatabaseConfigurationException("database.pool.validation-timeout-ms must be greater than 0.");
        }
        if (type == DatabaseType.MYSQL && pool.minimumIdle() > pool.maximumPoolSize()) {
            throw new DatabaseConfigurationException(
                    "database.pool.minimum-idle must not be greater than database.pool.maximum-pool-size for MySQL."
            );
        }
    }

    private static void requireNotBlank(String value, String path) {
        if (trimToNull(value) == null) {
            throw new DatabaseConfigurationException(path + " must not be blank when database.type=mysql.");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public boolean enabled() {
        return enabled;
    }

    public DatabaseType type() {
        return type;
    }

    public File sqliteFile() {
        return sqliteFile;
    }

    public MySqlSettings mysql() {
        return mysql;
    }

    public PoolSettings pool() {
        return pool;
    }

    public String describeTarget() {
        return switch (type) {
            case SQLITE -> sqliteFile.getAbsolutePath();
            case MYSQL -> mysql.host() + ":" + mysql.port() + "/" + mysql.database();
        };
    }

    public record MySqlSettings(
            String host,
            int port,
            String database,
            String username,
            String password,
            String sslMode,
            Map<String, String> properties
    ) {
        public MySqlSettings {
            properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        }
    }

    public record PoolSettings(
            int maximumPoolSize,
            int minimumIdle,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs,
            long keepaliveTimeMs,
            long validationTimeoutMs
    ) {
    }
}
