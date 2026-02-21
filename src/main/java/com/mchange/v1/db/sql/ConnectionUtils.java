package com.mchange.v1.db.sql;

import java.sql.*;
import com.mchange.v2.log.*;

public final class ConnectionUtils
{
    private final static MLogger logger = MLog.getLogger( ConnectionUtils.class );

    /** 
     * @return false iff and Exception occurred while
     *         trying to close this object.
     */
    public static boolean attemptClose(Connection con)
    {
	try 
 	    {
 		if (con != null) con.close();
		//System.err.println("Connection [ " + con + " ] closed.");
 		return true;
 	    }
        catch (SQLException e)
	    {
		//e.printStackTrace();
		//System.err.println("Connection close FAILED.");

		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.log( MLevel.WARNING, "Connection close FAILED.", e );
 		return false;
	    }
    }

    public static boolean attemptRollback(Connection con)
    {
        try 
	    {
		if (con != null) con.rollback();
		return true;
	    }
        catch (SQLException e)
            {
		//e.printStackTrace();

		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.log( MLevel.WARNING, "Rollback FAILED.", e );
		return false;
	    }
    }

    private ConnectionUtils()
    {}
}
