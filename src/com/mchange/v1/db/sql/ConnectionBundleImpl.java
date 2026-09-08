package com.mchange.v1.db.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

public class ConnectionBundleImpl implements ConnectionBundle
{
    Connection con;
    Map<String,PreparedStatement> map = new HashMap<String,PreparedStatement>();

    public ConnectionBundleImpl(Connection con)
    {this.con = con;}

    @Override
    public Connection getConnection()
    {return con;}

    @Override
    public PreparedStatement getStatement(String stmt_name)
    {return map.get(stmt_name);}

    @Override
    public void putStatement(String stmt_name, PreparedStatement stmt)
    {map.put(stmt_name, stmt);}

    @Override
    public void close() throws SQLException
    {this.con.close();}

    @Override
    @SuppressWarnings("deprecation") // a close() safety net; finalize() is deprecated but still called
    public void finalize() throws Exception
    {if (!con.isClosed()) this.close();}
}
