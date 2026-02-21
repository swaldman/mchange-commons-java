package com.mchange.v1.db.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

public class ConnectionBundleImpl implements ConnectionBundle
{
    Connection con;
    Map map = new HashMap();

    public ConnectionBundleImpl(Connection con)
    {this.con = con;}

    public Connection getConnection()
    {return con;}

    public PreparedStatement getStatement(String stmt_name)
    {return (PreparedStatement) map.get(stmt_name);}

    public void putStatement(String stmt_name, PreparedStatement stmt)
    {map.put(stmt_name, stmt);}

    public void close() throws SQLException
    {this.con.close();}

    public void finalize() throws Exception
    {if (!con.isClosed()) this.close();}
}
