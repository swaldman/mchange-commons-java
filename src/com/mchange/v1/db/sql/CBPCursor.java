package com.mchange.v1.db.sql;

import java.sql.*;

public abstract class CBPCursor extends SimpleCursor
{
    ConnectionBundle     returnMe;
    ConnectionBundlePool home;

    public CBPCursor(ResultSet rs, ConnectionBundle returnMe, ConnectionBundlePool home)
    {
	super(rs);
	this.returnMe = returnMe;
	this.home     = home;
    }

    public void close() throws Exception 
    {
	try
	    {super.close();}
	finally
	    {home.checkinBundle(returnMe);}
    }
}
