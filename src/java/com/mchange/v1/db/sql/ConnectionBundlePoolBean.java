/*
 * Distributed as part of mchange-commons-java v.0.2.3.3
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
import com.mchange.v1.util.BrokenObjectException;

public class ConnectionBundlePoolBean implements ConnectionBundlePool
{
    ConnectionBundlePool inner;

    public void init(String jdbcDriverClass, 
		     String jdbcUrl, String username, String pwd, 
		     int start, int max, int inc)
	throws SQLException, ClassNotFoundException
    {
	Class.forName( jdbcDriverClass );
	this.init(jdbcUrl, username, pwd, start, max, inc);
    }

    public void init(String jdbcUrl, String username, String pwd, int start, int max, int inc)
	throws SQLException
    {
	this.inner = new InnerPool(jdbcUrl, username, pwd, start, max, inc);
    }

    public ConnectionBundle checkoutBundle() 
	throws SQLException, InterruptedException, BrokenObjectException
    { return inner.checkoutBundle(); }
	
    public void checkinBundle(ConnectionBundle bndl) throws SQLException, BrokenObjectException
    { inner.checkinBundle(bndl); }
	
    public void close() throws SQLException
    { inner.close(); }

    protected void setConnectionOptions(Connection con) throws SQLException
    { con.setAutoCommit( false ); }

    class InnerPool extends ConnectionBundlePoolImpl
    {
	InnerPool(String jdbcUrl, String username, String pwd, int start, int max, int inc)
	    throws SQLException
	{
	    super(start, max, inc);

	    //ConnectionBundlePoolBean.this is not set until
	    //after superconstructor has completed, so we must
	    //put off init()
	    this.init(jdbcUrl, username, pwd);
	}
	
	protected void setConnectionOptions(Connection con) throws SQLException
	    { ConnectionBundlePoolBean.this.setConnectionOptions( con ); }
    }
}
