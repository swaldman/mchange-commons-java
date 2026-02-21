package com.mchange.v2.log.jdk14logging;

import java.util.*;
import java.util.logging.*;
import com.mchange.v2.log.*;

public final class ForwardingLogger extends Logger
{
    MLogger forwardTo;

    public ForwardingLogger( MLogger forwardTo, String resourceBundleName )
    {
	super( forwardTo.getName(), resourceBundleName );
	this.forwardTo = forwardTo;
    }

    public void log(LogRecord lr)
    {
	Level  lvl = lr.getLevel();
	MLevel mlvl = Jdk14LoggingUtils.mlevelFromLevel( lvl );

	String   rbName = lr.getResourceBundleName();
	String   msg    = lr.getMessage();
	Object[] params = lr.getParameters();

	String finalMsg = LogUtils.formatMessage( rbName, msg, params ); // robust to nulls

	Throwable t = lr.getThrown(); // may be null

	String scn = lr.getSourceClassName();
	String smn = lr.getSourceMethodName();

	boolean uses_srcloc = (scn != null & smn != null);

	if ( !uses_srcloc )
	    forwardTo.log( mlvl, finalMsg, t );
	else
	    forwardTo.logp( mlvl, scn, smn, finalMsg, t );
    }
}
