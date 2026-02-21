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
