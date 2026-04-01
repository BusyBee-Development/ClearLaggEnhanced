package com.clearlagenhanced.database.dialect;

public class MySqlDialect implements DatabaseDialect {

    @Override
    public String autoIncrementPrimaryKey() {
        return "BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY";
    }

    @Override
    public String name() {
        return "MySQL";
    }
}
