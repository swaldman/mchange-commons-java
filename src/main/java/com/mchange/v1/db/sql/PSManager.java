package com.mchange.v1.db.sql;

import java.sql.*;

public interface PSManager
{
    public PreparedStatement getPS(Connection con, String stmt_name);
    public void              putPS(Connection con, String name, PreparedStatement stmt);
}
