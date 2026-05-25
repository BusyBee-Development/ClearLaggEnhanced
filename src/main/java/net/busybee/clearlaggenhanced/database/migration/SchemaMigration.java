package net.busybee.clearlaggenhanced.database.migration;

import net.busybee.clearlaggenhanced.database.dialect.DatabaseDialect;

import java.sql.Connection;
import java.sql.SQLException;

public interface SchemaMigration {

    int version();

    String description();

    void apply(Connection connection, DatabaseDialect dialect) throws SQLException;
}
