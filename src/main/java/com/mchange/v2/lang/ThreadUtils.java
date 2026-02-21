package com.mchange.v2.lang;

import com.mchange.v2.log.*;
import java.lang.reflect.Method;

public final class ThreadUtils
{
    private final static MLogger logger = MLog.getLogger( ThreadUtils.class );

    final static Method holdsLock;

    static
    {
	Method _holdsLock;
	try
	    { _holdsLock = Thread.class.getMethod("holdsLock", new Class[] { Object.class }); }
	catch (NoSuchMethodException e)
	    { _holdsLock = null; }

	holdsLock = _holdsLock;
    }

    public static void enumerateAll( Thread[] threads )
    { ThreadGroupUtils.rootThreadGroup().enumerate( threads ); }

    /**
     * @return null if cannot be determined, otherwise true or false
     */
    public static Boolean reflectiveHoldsLock( Object o )
    {
	try
	    {
		if (holdsLock == null)
		    return null;
		else
		    return (Boolean) holdsLock.invoke( null, new Object[] { o } );
	    }
	catch (Exception e)
	    {
		if ( logger.isLoggable( MLevel.FINER ) )
		    logger.log( MLevel.FINER, "An Exception occurred while trying to call Thread.holdsLock( ... ) reflectively.", e);
		return null;
	    }
    }

    private ThreadUtils()
    {}
}
