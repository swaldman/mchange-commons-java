package com.mchange.v1.db.sql;

import java.sql.*;
import com.mchange.v1.util.ClosableResource;

public interface ConnectionBundle extends ClosableResource
{
    public Connection        getConnection();
    public PreparedStatement getStatement(String stmt_name);
    public void              putStatement(String stmt_name, PreparedStatement stmt);
}
