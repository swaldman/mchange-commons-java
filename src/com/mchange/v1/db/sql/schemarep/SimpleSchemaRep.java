package com.mchange.v1.db.sql.schemarep;

import java.util.*;

public interface SimpleSchemaRep
{
    public Set getTableNames();
    public TableRep tableRepForName(String name);
}
