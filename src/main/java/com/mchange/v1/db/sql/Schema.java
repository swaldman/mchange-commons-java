package com.mchange.v1.db.sql;

import java.sql.*;

public interface Schema
{
    public void createSchema(Connection con) throws SQLException;
    public void dropSchema(Connection con) throws SQLException;

    public String getStatementText(String appName, String stmtName);
}
