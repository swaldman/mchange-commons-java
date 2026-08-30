package com.mchange.v1.util;

import com.mchange.v2.log.*;

public final class ClosableResourceUtils
{
    private final static MLogger logger = MLog.getLogger( ClosableResourceUtils.class );

    /**
     * attempts to close the specified resource,
     * logging any exception or failure, but allowing
     * control flow to proceed normally regardless.
     */
    public static Exception attemptClose(ClosableResource cr)
    {
	try
	    {
		if (cr != null) cr.close();
		return null;
	    }
	catch (Exception e)
	    {
		//e.printStackTrace();
		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.log( MLevel.WARNING, "CloseableResource close FAILED.", e );
		return e;
	    }
    }

    private ClosableResourceUtils()
    {}
}
