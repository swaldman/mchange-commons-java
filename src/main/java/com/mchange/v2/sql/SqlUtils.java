/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

package com.mchange.v2.sql;

import java.sql.*;
import com.mchange.v2.log.*;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import com.mchange.lang.ThrowableUtils;
import com.mchange.v2.lang.VersionUtils;

public final class SqlUtils
{
    final static MLogger logger = MLog.getLogger( SqlUtils.class );

    // protected by SqlUtils.class' lock
    final static DateFormat tsdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");

    public final static String DRIVER_MANAGER_USER_PROPERTY     = "user";
    public final static String DRIVER_MANAGER_PASSWORD_PROPERTY = "password";

    public static String escapeBadSqlPatternChars(String s)
    {
	StringBuffer sb = new StringBuffer(s);
	for (int i = 0, len = sb.length(); i < len; ++i)
	    if (sb.charAt(i) == '\'')
		{
		    sb.insert(i, '\'');
		    ++len;
		    i+=2;
		}
	return sb.toString();
    }

    public synchronized static String escapeAsTimestamp( Date date )
    { return "{ts '" + tsdf.format( date ) + "'}";  }

    public static SQLException toSQLException(Throwable t)
    { return toSQLException(null, t ); }

    public static SQLException toSQLException(String msg, Throwable t)
    { return toSQLException(msg, null, t);}

    public static SQLException toSQLException(String msg, String sqlState, Throwable t)
    {
        if (t instanceof SQLException)
	    {
		if (Debug.DEBUG && 
		    Debug.TRACE == Debug.TRACE_MAX && 
		    logger.isLoggable( MLevel.FINER ))
		    {
			SQLException s = (SQLException) t;
			StringBuffer tmp = new StringBuffer(255);
			tmp.append("Attempted to convert SQLException to SQLException. Leaving it alone.");
			tmp.append(" [SQLState: ");
			tmp.append( s.getSQLState() );
			tmp.append("; errorCode: " );
			tmp.append( s.getErrorCode() );
			tmp.append(']');
			if (msg != null)
			    tmp.append(" Ignoring suggested message: '" + msg + "'.");
			logger.log( MLevel.FINER, tmp.toString(), t );

			SQLException s2 = s;
			while ((s2 = s2.getNextException()) != null)
			    logger.log( MLevel.FINER, "Nested SQLException or SQLWarning: ", s2 );
		    }
		return (SQLException) t;
	    }
        else
        { 
            if (Debug.DEBUG) 
		{
		    //t.printStackTrace();
		    if ( logger.isLoggable( MLevel.FINE ) )
			logger.log( MLevel.FINE, "Converting Throwable to SQLException...", t );
		}

	    if (msg == null)
		msg = "An SQLException was provoked by the following failure: " + t.toString();
	    if ( VersionUtils.isAtLeastJavaVersion14() )
		{
		    SQLException out = new SQLException(msg);
		    out.initCause( t );
		    return out;
		}
	    else
		return new SQLException( msg + System.getProperty( "line.separator" ) +
					 "[Cause: " + ThrowableUtils.extractStackTrace(t) + ']', sqlState); 
        }
    }

    public static SQLClientInfoException toSQLClientInfoException(Throwable t)
    {
	if (t instanceof SQLClientInfoException)
	    return (SQLClientInfoException) t;
	else if (t.getCause() instanceof SQLClientInfoException)
	    return (SQLClientInfoException) t.getCause();
	else if (t instanceof SQLException)
	{
	    SQLException sqle = (SQLException) t;
	    return new SQLClientInfoException(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode(), null, t);
	}
	else
	    return new SQLClientInfoException(t.getMessage(), null, t);
    }

    private SqlUtils()
    {}
}
