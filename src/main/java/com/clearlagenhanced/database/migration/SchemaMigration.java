package com.clearlagenhanced.database.migration;

import com.clearlagenhanced.database.dialect.DatabaseDialect;

import java.sql.Connection;
import java.sql.SQLException;

public interface SchemaMigration {

    int version();

    String description();

    void apply(Connection connection, DatabaseDialect dialect) throws SQLException;
}
