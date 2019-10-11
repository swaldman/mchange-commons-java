package com.mchange.v2.log.log4j2;

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
//import org.apache.logging.log4j.core.filter.ThresholdFilter;

import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLevel;


/**
 *  A quick and dirty bridge for libraries (like sbt 1.x) that use log4j2 Appenders as generic loggers
 */
public class MLogAppender extends AbstractAppender
{

    public MLogAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions)
    {
	super(name, filter, layout, ignoreExceptions);
	this.start();
    }

    public MLogAppender(final String name )
    {
	this(
	     name,
	     null, //ThresholdFilter.createFilter( Level.ALL, Filter.Result.ACCEPT, Filter.Result.NEUTRAL ),
	     null,
	     false
	     );
    }

    private MLevel levelToMLevel( Level level )
    {
	if ( level == Level.OFF )        return MLevel.OFF;
	else if ( level == Level.FATAL ) return MLevel.SEVERE;
	else if ( level == Level.ERROR ) return MLevel.SEVERE;
	else if ( level == Level.WARN )  return MLevel.WARNING;
	else if ( level == Level.INFO )  return MLevel.INFO;
	else if ( level == Level.DEBUG ) return MLevel.DEBUG;
	else if ( level == Level.TRACE ) return MLevel.TRACE;
	else if ( level == Level.ALL )   return MLevel.ALL;
	else throw new IllegalArgumentException( "Unknown log4j2 Level: " + level );
    }

    @Override
    public final void append(final LogEvent event)
    { MLog.getLogger( this.getName() ).log( levelToMLevel( event.getLevel()), event.getMessage().getFormattedMessage(), event.getThrown() ); }  
}
