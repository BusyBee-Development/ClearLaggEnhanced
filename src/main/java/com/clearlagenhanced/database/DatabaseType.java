package com.clearlagenhanced.database;

import java.util.Locale;

public enum DatabaseType {
    SQLITE("sqlite"),
    MYSQL("mysql");

    private final String configValue;

    DatabaseType(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigValue() {
        return configValue;
    }

    public String getDisplayName() {
        return switch (this) {
            case SQLITE -> "SQLite";
            case MYSQL -> "MySQL";
        };
    }

    public static DatabaseType fromConfigValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new DatabaseConfigurationException("Invalid database.type ''. Accepted values: sqlite, mysql.");
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        for (DatabaseType type : values()) {
            if (type.configValue.equals(normalized)) {
                return type;
            }
        }

        throw new DatabaseConfigurationException(
                "Invalid database.type '" + rawValue + "'. Accepted values: sqlite, mysql."
        );
    }

    public static DatabaseType fromConfigValueOrDefault(String rawValue, DatabaseType fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        for (DatabaseType type : values()) {
            if (type.configValue.equals(normalized)) {
                return type;
            }
        }

        return fallback;
    }
}
