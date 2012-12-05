/*
 * Distributed as part of mchange-commons-java v.0.2.3.2
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
