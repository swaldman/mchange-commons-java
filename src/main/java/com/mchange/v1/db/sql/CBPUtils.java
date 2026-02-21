package com.mchange.v1.db.sql;

public class CBPUtils
{
    public static void attemptCheckin(ConnectionBundle cb, ConnectionBundlePool cbp)
    {
	try
	    {cbp.checkinBundle(cb);}
	catch (Exception e)
	    {e.printStackTrace();}
    }

}
