package com.clearlagenhanced.database.dialect;

public interface DatabaseDialect {

    String autoIncrementPrimaryKey();

    String name();
}
