package com.clearlagenhanced.database.datasource;

import com.clearlagenhanced.database.DatabaseSettings;
import com.zaxxer.hikari.HikariDataSource;

public interface DataSourceFactory {

    HikariDataSource create(String pluginName, DatabaseSettings settings);
}
