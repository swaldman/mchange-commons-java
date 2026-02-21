package com.mchange.v1.db.sql.schemarep;

public interface ColumnRep
{
    public String  getColumnName();
    public int     getColumnType();
    public int[]   getColumnSize();
    public boolean acceptsNulls();

    /** @return null for no default */
    public Object getDefaultValue();
}
