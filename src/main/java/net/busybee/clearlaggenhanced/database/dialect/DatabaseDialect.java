package net.busybee.clearlaggenhanced.database.dialect;

public interface DatabaseDialect {

    String autoIncrementPrimaryKey();

    String name();
}
