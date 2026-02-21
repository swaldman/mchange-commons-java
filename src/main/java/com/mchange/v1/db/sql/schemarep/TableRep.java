package com.mchange.v1.db.sql.schemarep;

import java.util.*;

public interface TableRep
{
    public String getTableName();
    public Iterator getColumnNames();
    public ColumnRep columnRepForName(String name);
    public Set getPrimaryKeyColumnNames();
    public Set getForeignKeyReps();
    public Set getUniquenessConstraintReps();
}
