package com.mchange.v1.db.sql;

import java.sql.*;
import java.util.*;

/*
 * I think this class is insufficient, because the 
 * values may be strongly referenced by the map, and
 * PreparedStatements may contain a backreference to
 * their connections.
 */
public class WeakHashPSManager implements PSManager
{
    WeakHashMap<Connection,Map<String,PreparedStatement>> wmap = new WeakHashMap<Connection,Map<String,PreparedStatement>>();

    @Override
    public PreparedStatement getPS(Connection con, String stmt_name)
    {
	Map<String,PreparedStatement> nameMap = wmap.get(con);
	return (nameMap == null ? null : nameMap.get(stmt_name));
    }

    @Override
    public void putPS(Connection con, String name, PreparedStatement stmt)
    {
	Map<String,PreparedStatement> nameMap = wmap.get(con);
	if (nameMap == null)
	    {
		nameMap = new HashMap<String,PreparedStatement>();
		wmap.put(con, nameMap);
	    }
	nameMap.put(name, stmt);
    }
}
