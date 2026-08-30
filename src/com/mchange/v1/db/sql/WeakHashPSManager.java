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
    WeakHashMap wmap = new WeakHashMap();

    public PreparedStatement getPS(Connection con, String stmt_name)
    {
	Map nameMap = (Map) wmap.get(con);
	return (nameMap == null ? null : (PreparedStatement) nameMap.get(stmt_name));
    }

    public void putPS(Connection con, String name, PreparedStatement stmt)
    {
	Map nameMap = (Map) wmap.get(con);
	if (nameMap == null)
	    {
		nameMap = new HashMap();
		wmap.put(con, nameMap);
	    }
	nameMap.put(name, stmt);
    }
}
