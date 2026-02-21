package com.mchange.v1.db.sql;

import java.sql.*;
import com.mchange.v2.log.*;

public final class StatementUtils
{
    private final static MLogger logger = MLog.getLogger( StatementUtils.class );

    /** 
     * @return false iff and Exception occurred while
     *         trying to close this object.
     */
    public static boolean attemptClose(Statement stmt)
    {
        try 
	    {
		if (stmt != null) stmt.close();
		return true;
	    }
        catch (SQLException e)
            {
		//e.printStackTrace();
		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.log( MLevel.WARNING, "Statement close FAILED.", e );
		return false;
	    }
    }

    private StatementUtils()
    {}
}
