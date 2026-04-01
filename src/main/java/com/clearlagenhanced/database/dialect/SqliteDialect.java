package com.clearlagenhanced.database.dialect;

public class SqliteDialect implements DatabaseDialect {

    @Override
    public String autoIncrementPrimaryKey() {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    public String name() {
        return "SQLite";
    }
}
