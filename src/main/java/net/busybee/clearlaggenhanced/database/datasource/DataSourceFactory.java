package net.busybee.clearlaggenhanced.database.datasource;

import net.busybee.clearlaggenhanced.database.DatabaseSettings;
import com.zaxxer.hikari.HikariDataSource;

public interface DataSourceFactory {

    HikariDataSource create(String pluginName, DatabaseSettings settings);
}
