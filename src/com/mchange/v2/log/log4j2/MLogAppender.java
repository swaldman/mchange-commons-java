package com.mchange.v2.log.log4j2;

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.message.Message;
//import org.apache.logging.log4j.core.filter.ThresholdFilter;

import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLevel;


/**
 *  A quick and dirty bridge for libraries (like sbt 1.x) that use log4j2 Appenders as generic loggers
 */
public class MLogAppender extends AbstractAppender
{

    //
    // This AbstractAppender constructor is deprecated in favor of an overload taking
    // a trailing Property[]. The deprecated one delegates straight to that overload
    // with Property.EMPTY_ARRAY, so switching would be behaviorally identical.
    //
    // It would not be free, though. log4j2 is an optional dependency: we compile
    // against 2.17.1, but a user runs whatever log4j2 their application already has.
    // Naming the Property[] constructor, and Property.EMPTY_ARRAY with it, would
    // require a log4j2 at least as new as those additions, turning an older log4j2
    // on the classpath into a NoSuchMethodError when this appender is constructed.
    // The deprecated constructor is still present as of 2.26.0.
    //
    // So the migration would buy no behavior and cost runtime compatibility.
    //
    @SuppressWarnings("deprecation")
    protected MLogAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout, final boolean ignoreExceptions)
    {
	super(name, filter, layout, ignoreExceptions);
	this.start();
    }

    public MLogAppender(String name, Filter filter)
    { this( name, filter, null, false ); }

    public MLogAppender(String name )
    {
	this( name, null ); //ThresholdFilter.createFilter( Level.ALL, Filter.Result.ACCEPT, Filter.Result.NEUTRAL ),
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
    { MLog.getLogger( this.getName() ).log( levelToMLevel( event.getLevel() ), messageToString( event.getMessage() ), event.getThrown() ); }

    public String messageToString( Message message )
    { return message.getFormattedMessage(); }
}
