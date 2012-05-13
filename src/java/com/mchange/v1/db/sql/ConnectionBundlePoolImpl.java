/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
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
import com.mchange.v1.util.*;

public abstract class ConnectionBundlePoolImpl extends AbstractResourcePool implements ConnectionBundlePool
{
    String jdbcUrl;
    String username;
    String pwd;

    public ConnectionBundlePoolImpl(String jdbcUrl, String username, String pwd,
				    int start, int max, int inc) 
	throws SQLException
    {
	super(start, max, inc);
	init(jdbcUrl, username, pwd);
    }

    protected ConnectionBundlePoolImpl(int start, int max, int inc)
    { super(start, max, inc); }

    protected void init(String jdbcUrl, String username, String pwd) throws SQLException
    {
	this.jdbcUrl = jdbcUrl;
	this.username = username;
	this.pwd = pwd;
	try {init();}
	catch (SQLException se)
	    {throw se;}
	catch (Exception e)
	    {throw new UnexpectedException(e, "Unexpected exception while initializing ConnectionBundlePool");}
    }

    public ConnectionBundle checkoutBundle() throws SQLException, BrokenObjectException, InterruptedException
    {
	try
	    {return (ConnectionBundle) this.checkoutResource();}
	catch (BrokenObjectException boe)
	    {throw boe;}
	catch (InterruptedException ie)
	    {throw ie;}
	catch (SQLException se)
	    {throw se;}
	catch (Exception e)
	    {throw new UnexpectedException(e, "Unexpected exception while checking out ConnectionBundle");}
    }

    public void checkinBundle(ConnectionBundle bndl) throws BrokenObjectException
    {this.checkinResource(bndl);}


    public void close() throws SQLException
    {
	try
	    {super.close();}
	catch (SQLException e)
	    {throw e;}
	catch (Exception e)
	    {throw new UnexpectedException(e, "Unexpected exception while closing pool.");}
    }
    
    protected Object acquireResource() throws Exception
    {
	Connection con = DriverManager.getConnection(jdbcUrl, username, pwd);
	setConnectionOptions(con);
	return new ConnectionBundleImpl(con);
    }

    protected void refurbishResource(Object resc) throws BrokenObjectException
    {
	boolean bad;
	try
	    {
		Connection con = ((ConnectionBundle) resc).getConnection();
		con.rollback(); //get rid of any uncompleted work that's been done
		bad = con.isClosed();
		setConnectionOptions(con);
	    }
	catch (SQLException e)
	    { bad = true; }
	if (bad) throw new BrokenObjectException(resc);
    }
	

    protected void destroyResource(Object resc) throws Exception
    {((ConnectionBundle) resc).close();}

    protected abstract void setConnectionOptions(Connection con) throws SQLException;
}

