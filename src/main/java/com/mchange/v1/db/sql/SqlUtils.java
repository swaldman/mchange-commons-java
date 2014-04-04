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
