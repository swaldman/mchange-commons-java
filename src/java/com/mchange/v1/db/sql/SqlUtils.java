/*
 * Distributed as part of mchange-commons-java v.0.2.4
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v1.db.sql;

import java.sql.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import com.mchange.lang.ThrowableUtils;

/**
 * @deprecated use com.mchange.v2.sql.SqlUtils
 */
public final class SqlUtils
{
    final static DateFormat tsdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");

    //TODO! Look up SQL escape syntax and really implement this!!!
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

    public static String escapeAsTimestamp( Date date )
    {
	//POSTGRES JDBC DOES APPARENTLY DOES NOT SUPPORT
	//THE ESCAPE.... factor stuff like this out into
	//dbms schema...
	return "{ts '" + tsdf.format( date ) + "'}"; 
	//return '\'' + tsdf.format( date ) + '\''; 
    }

    public static SQLException toSQLException(Throwable t)
    {
        if (t instanceof SQLException)
            return (SQLException) t;
        else
        { 
            if (Debug.DEBUG) t.printStackTrace();
            return new SQLException( ThrowableUtils.extractStackTrace(t) ); 
        }
    }
    
    private SqlUtils()
    {}
}
