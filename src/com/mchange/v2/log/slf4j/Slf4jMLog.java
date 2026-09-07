package com.mchange.v2.log.slf4j;

import java.text.*;
import java.util.*;

import org.slf4j.*;
import com.mchange.v2.log.*;

import static com.mchange.v2.log.MLevel.*;
import static com.mchange.v2.log.LogUtils.*;

public final class Slf4jMLog extends MLog
{
    final static Object[] EMPTY_OBJ_ARRAY = new Object[0];

    private final static int ALL_INTVAL     = ALL.intValue();
    private final static int CONFIG_INTVAL  = CONFIG.intValue();
    private final static int FINE_INTVAL    = FINE.intValue();
    private final static int FINER_INTVAL   = FINER.intValue();
    private final static int FINEST_INTVAL  = FINEST.intValue();
    private final static int INFO_INTVAL    = INFO.intValue();
    private final static int OFF_INTVAL     = OFF.intValue();
    private final static int SEVERE_INTVAL  = SEVERE.intValue();
    private final static int WARNING_INTVAL = WARNING.intValue();

    final static String CHECK_CLASS = "org.slf4j.LoggerFactory";

    final static String DFLT_LOGGER_NAME = "global";

    public Slf4jMLog() throws ClassNotFoundException, MLogInitializationException
    { 
	Class.forName( CHECK_CLASS );
	ILoggerFactory ilf = LoggerFactory.getILoggerFactory();
	if ( ilf == null || ilf.getClass().getName() == "org.slf4j.helpers.NOPLoggerFactory" ) // if NOP (no-op) logger is configured, that means no meaningful binding is available
	    throw new MLogInitializationException("slf4j found no binding or threatened to use its (dangerously silent) NOPLogger. We consider the slf4j library not found.");
    }

    @Override
    public MLogger getMLogger(String name)
    {
        Logger lg = LoggerFactory.getLogger(name);
        if (lg == null)
        {
            fallbackWarn(" with name '" + name + "'");
            return NullMLogger.instance();
        }
        else
            return new Slf4jMLogger( lg ); 
    }

    @Override
    public MLogger getMLogger()
    {
        Logger lg = LoggerFactory.getLogger(DFLT_LOGGER_NAME);
        if (lg == null)
        {
            fallbackWarn(" (default, with name '" + DFLT_LOGGER_NAME + "')");
            return NullMLogger.instance();
        }
        else
            return new Slf4jMLogger( lg ); 
    }

    private void fallbackWarn(String subst)
    {
        FallbackMLog.getLogger().warning("Could not create or find slf4j Logger" + subst + ". " +
                                         "Using NullMLogger. All messages sent to this" +
                                         "logger will be silently ignored. You might want to fix this.");
    }


    private final static class Slf4jMLogger implements MLogger
    {
	final static String FQCN = Slf4jMLogger.class.getName();
	
	final Logger logger;

	final LevelLogger traceL;
	final LevelLogger debugL;
	final LevelLogger infoL;
	final LevelLogger warnL;
	final LevelLogger errorL;
	final LevelLogger offL;


        // protected by this' lock
        MLevel myLevel = null;
	
	Slf4jMLogger( Logger logger )
	{ 
	    this.logger = logger; 
	    this.traceL = new TraceLogger();
	    this.debugL = new DebugLogger();
	    this.infoL  = new InfoLogger();
	    this.warnL  = new WarnLogger();
	    this.errorL = new ErrorLogger();
	    this.offL   = new OffLogger();
	}
	
	private MLevel guessMLevel()
        {
	    if ( logger.isTraceEnabled() )
		return MLevel.FINEST;
	    else if ( logger.isDebugEnabled() )
		return MLevel.FINER;
	    else if ( logger.isInfoEnabled() )
		return MLevel.INFO;
	    else if ( logger.isWarnEnabled() )
		return MLevel.WARNING;
	    else if ( logger.isErrorEnabled() )
		return MLevel.SEVERE;
	    else
		return MLevel.OFF;
        }

	private synchronized boolean myLevelMayBeLoggable( int intval )
	{ return ( myLevel == null || intval >= myLevel.intValue() ); }

	private LevelLogger levelLogger( MLevel l )
	{
	    LevelLogger outL = offL; // if nothing is proved loggable, we return the non-logger

	    int n = l.intValue();

	    // if a log level has been explicitly set on this logger
	    // and the level we are asked to log at is below this level,
	    // we know we should not log anything, i.e. stick with OffLogger
	    if ( myLevelMayBeLoggable( n ) && n >= FINEST_INTVAL) 
	    {
		if (n < FINER_INTVAL) 
		    { if (logger.isTraceEnabled()) outL = traceL; }
		else if (n < INFO_INTVAL) 
		    { if (logger.isDebugEnabled()) outL = debugL; }
		else if (n < WARNING_INTVAL)
		    { if (logger.isInfoEnabled()) outL = infoL; }
		else if (n < SEVERE_INTVAL)
		    { if (logger.isWarnEnabled()) outL = warnL; }
		else  if (n < OFF_INTVAL)
		    { if (logger.isErrorEnabled()) outL = errorL; }
            }

	    return outL;
	}

	private interface LevelLogger
	{
	    public void log( String msg );
	    public void log( String format, Object param);
	    public void log( String format, Object[] param);
	    public void log( String msg, Throwable t );
	}

	private class OffLogger implements LevelLogger
	{
	    @Override
	    public void log( String msg )                     {}
	    @Override
	    public void log( String format, Object param)     {}
	    @Override
	    public void log( String format, Object[] params)  {}
	    @Override
	    public void log( String msg, Throwable t )        {}
	}

	private class TraceLogger implements LevelLogger
	{
	    @Override
	    public void log( String msg )                     { logger.trace( msg ); }
	    @Override
	    public void log( String format, Object param)     { logger.trace( format, param ); }
	    @Override
	    public void log( String format, Object[] params)  { logger.trace( format, params ); }
	    @Override
	    public void log( String msg, Throwable t )        { logger.trace( msg, t ); }
	}

	private class DebugLogger implements LevelLogger
	{
	    @Override
	    public void log( String msg )                     { logger.debug( msg ); }
	    @Override
	    public void log( String format, Object param)     { logger.debug( format, param ); }
	    @Override
	    public void log( String format, Object[] params)  { logger.debug( format, params ); }
	    @Override
	    public void log( String msg, Throwable t )        { logger.debug( msg, t ); }
	}

	private class InfoLogger implements LevelLogger
	{
	    @Override
	    public void log( String msg )                     { logger.info( msg ); }
	    @Override
	    public void log( String format, Object param)     { logger.info( format, param ); }
	    @Override
	    public void log( String format, Object[] params)  { logger.info( format, params ); }
	    @Override
	    public void log( String msg, Throwable t )        { logger.info( msg, t ); }
	}

	private class WarnLogger implements LevelLogger
	{
	    @Override
	    public void log( String msg )                     { logger.warn( msg ); }
	    @Override
	    public void log( String format, Object param)     { logger.warn( format, param ); }
	    @Override
	    public void log( String format, Object[] params)  { logger.warn( format, params ); }
	    @Override
	    public void log( String msg, Throwable t )        { logger.warn( msg, t ); }
	}
	
	private class ErrorLogger implements LevelLogger
	{
	    @Override
	    public void log( String msg )                     { logger.error( msg ); }
	    @Override
	    public void log( String format, Object param)     { logger.error( format, param ); }
	    @Override
	    public void log( String format, Object[] params)  { logger.error( format, params ); }
	    @Override
	    public void log( String msg, Throwable t )        { logger.error( msg, t ); }
	}

        @Override
        @Deprecated
        public ResourceBundle getResourceBundle()
        { return null; }

        @Override
        @Deprecated
        public String getResourceBundleName()
        { return null; }

        @Override
        @Deprecated
        public void setFilter(Object java14Filter) throws SecurityException
        { warning("setFilter() not supported by MLogger " + this.getClass().getName()); }

        @Override
        @Deprecated
        public Object getFilter()
        { return null; }

        @Override
        public void log(MLevel l, String msg)
	{ levelLogger( l ).log( msg ); }

        @Override
        public void log(MLevel l, String msg, Object param)
        { levelLogger( l ).log( msg, param ); }

        @Override
        public void log(MLevel l,String msg, Object[] params)
	{ levelLogger( l ).log( msg, params ); }

        @Override
        public void log(MLevel l, String msg, Throwable t)
	{ levelLogger( l ).log( msg, t ); }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, msg) ); }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param)
        { levelLogger(l).log(  createMessage( srcClass, srcMeth, (msg!=null ? MessageFormat.format(msg, new Object[] {param}) : null) ) ); }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, (msg!=null ? MessageFormat.format(msg, params) : null) ) ); }

        @Override
        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, msg ),  t); }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, formatMessage(rb, msg, null) ) ); }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, formatMessage(rb, msg, new Object[] { param } ) ) ); }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, formatMessage(rb, msg, params) ) ); }

        @Override
        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, formatMessage(rb, msg, null) ),  t); }

        @Override
        public void entering(String srcClass, String srcMeth)
        { traceL.log( createMessage( srcClass, srcMeth, "entering method." ) ); }

        @Override
        public void entering(String srcClass, String srcMeth, Object param)
        { traceL.log( createMessage( srcClass, srcMeth, "entering method... param: " + param.toString() ) ); }

        @Override
        public void entering(String srcClass, String srcMeth, Object params[])
        { traceL.log( createMessage( srcClass, srcMeth, "entering method... " + LogUtils.createParamsList( params ) ) ); }

        @Override
        public void exiting(String srcClass, String srcMeth)
        { traceL.log( createMessage( srcClass, srcMeth, "exiting method." ) ); }

        @Override
        public void exiting(String srcClass, String srcMeth, Object result)
        { traceL.log( createMessage( srcClass, srcMeth, "exiting method... result: " + result.toString() ) ); }

        @Override
        public void throwing(String srcClass, String srcMeth, Throwable t)
        { traceL.log( createMessage( srcClass, srcMeth, "throwing exception... " ),  t); }

        @Override
        public void severe(String msg)
        { errorL.log( msg ); }

        @Override
        public void warning(String msg)
        { warnL.log( msg ); }

        @Override
        public void info(String msg)
        { infoL.log( msg ); }

        @Override
        public void config(String msg)
        { debugL.log( msg ); }

        @Override
        public void fine(String msg)
        { debugL.log( msg ); }

        @Override
        public void finer(String msg)
        { debugL.log( msg ); }

        @Override
        public void finest(String msg)
        { traceL.log( msg ); }

        @Override
        @Deprecated
        public synchronized void setLevel(MLevel l) throws SecurityException
        { myLevel = l; }

        @Override
        @Deprecated
        public synchronized MLevel getLevel()
        { 
            if (myLevel == null)
                myLevel = guessMLevel();
            return myLevel;
        }

        @Override
        public boolean isLoggable(MLevel l)
        { return levelLogger( l ) != offL; }

        @Override
        public String getName()
        { return logger.getName(); }

        @Override
        @Deprecated
        public void addHandler(Object h) throws SecurityException
        { 
	    throw new UnsupportedOperationException("Handlers not supported; the 'handler' " + h + " is not compatible with MLogger " + this); 
        }

        @Override
        @Deprecated
        public void removeHandler(Object h) throws SecurityException
        {
	    throw new UnsupportedOperationException("Handlers not supported; the 'handler' " + h + " is not compatible with MLogger " + this); 
        }

        @Override
        @Deprecated
        public Object[] getHandlers()
        { return EMPTY_OBJ_ARRAY; }

        @Override
        @Deprecated
        public void setUseParentHandlers(boolean uph)
        { throw new UnsupportedOperationException("Handlers not supported."); }

        @Override
        @Deprecated
        public boolean getUseParentHandlers()
        { throw new UnsupportedOperationException("Handlers not supported."); }
    }
}
