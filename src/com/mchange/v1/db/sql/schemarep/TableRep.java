package com.mchange.v1.db.sql.schemarep;

import java.util.*;

public interface TableRep
{
    public String getTableName();
    public Iterator<String> getColumnNames();
    public ColumnRep columnRepForName(String name);
    public Set<String> getPrimaryKeyColumnNames();
    public Set<ForeignKeyRep> getForeignKeyReps();
    public Set<UniquenessConstraintRep> getUniquenessConstraintReps();
}
