package com.mchange.v1.util;

import java.sql.*;

/**
 *  @deprecated use functions in per-closeable resouce utility classes
 */
public final class CleanupUtils
{

    public static void attemptClose(Statement stmt)
    {
	try {if (stmt != null) stmt.close();}
	catch (SQLException e)
	    {e.printStackTrace();}
    }

    public static void attemptClose(Connection con)
    {
	try {if (con != null) con.close();}
	catch (SQLException e)
	    {e.printStackTrace();}
    }

    public static void attemptRollback(Connection con)
    {
	try {if (con != null) con.rollback();}
	catch (SQLException e)
	    {e.printStackTrace();}
    }

    private CleanupUtils()
    {}
}
