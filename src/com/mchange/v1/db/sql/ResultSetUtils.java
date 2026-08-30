package com.mchange.v1.db.sql;

import java.sql.*;
import com.mchange.v2.log.*;

public final class ResultSetUtils
{
    private final static MLogger logger = MLog.getLogger( ResultSetUtils.class );

    /** 
     * @return false iff and Exception occurred while
     *         trying to close this object.
     */
    public static boolean attemptClose(ResultSet rs)
    {
        try 
	    {
		if (rs != null) rs.close();
		return true;
	    }
        catch (SQLException e)
            {
		//e.printStackTrace();
		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.log( MLevel.WARNING, "ResultSet close FAILED.", e );
		return false;
	    }
    }

    private ResultSetUtils()
    {}
}
