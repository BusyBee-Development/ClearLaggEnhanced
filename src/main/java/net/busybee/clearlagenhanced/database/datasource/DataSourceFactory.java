package net.busybee.clearlagenhanced.database.datasource;

import net.busybee.clearlagenhanced.database.DatabaseSettings;
import com.zaxxer.hikari.HikariDataSource;

public interface DataSourceFactory {

    HikariDataSource create(String pluginName, DatabaseSettings settings);
}
