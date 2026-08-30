package com.mchange.v2.log.jdk14logging;

import java.util.*;
import java.util.logging.*;
import com.mchange.v2.log.*;

public final class Jdk14LoggingUtils
{
    public static MLevel mlevelFromLevel( Level lvl )
    {
	if ( lvl == Level.ALL )
	    return MLevel.ALL;
	else if ( lvl == Level.CONFIG )
	    return MLevel.CONFIG;
	else if ( lvl == Level.FINE )
	    return MLevel.FINE;
	else if ( lvl == Level.FINER )
	    return MLevel.FINER;
	else if ( lvl == Level.FINEST )
	    return MLevel.FINEST;
	else if ( lvl == Level.INFO )
	    return MLevel.INFO;
	else if ( lvl == Level.OFF )
	    return MLevel.OFF;
	else if ( lvl == Level.SEVERE )
	    return MLevel.SEVERE;
	else if ( lvl == Level.WARNING )
	    return MLevel.WARNING;
	else 
	    throw new IllegalArgumentException("Unexpected Jdk14 logging level: " + lvl);
    }

    public static Level levelFromMLevel( MLevel mlvl )
    {
	if ( mlvl == MLevel.ALL )
	    return Level.ALL;
	else if ( mlvl == MLevel.CONFIG )
	    return Level.CONFIG;
	else if ( mlvl == MLevel.FINE )
	    return Level.FINE;
	else if ( mlvl == MLevel.FINER )
	    return Level.FINER;
	else if ( mlvl == MLevel.FINEST )
	    return Level.FINEST;
	else if ( mlvl == MLevel.INFO )
	    return Level.INFO;
	else if ( mlvl == MLevel.OFF )
	    return Level.OFF;
	else if ( mlvl == MLevel.SEVERE )
	    return Level.SEVERE;
	else if ( mlvl == MLevel.WARNING )
	    return Level.WARNING;
	else 
	    throw new IllegalArgumentException("Unexpected MLevel: " + mlvl);
    }

    private Jdk14LoggingUtils()
    {}
}
