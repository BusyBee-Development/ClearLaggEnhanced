package net.busybee.clearlagenhanced.database.dialect;

public interface DatabaseDialect {

    String autoIncrementPrimaryKey();

    String name();
}
