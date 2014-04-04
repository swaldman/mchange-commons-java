/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

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

    public Slf4jMLog() throws ClassNotFoundException
    { Class.forName( CHECK_CLASS ); }

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
	    if ( logger.isErrorEnabled() )
		return MLevel.SEVERE;
	    else if ( logger.isWarnEnabled() )
		return MLevel.WARNING;
	    else if ( logger.isInfoEnabled() )
		return MLevel.INFO;
	    else if ( logger.isDebugEnabled() )
		return MLevel.FINER;
	    else if ( logger.isTraceEnabled() )
		return MLevel.FINEST;
	    else
		return MLevel.OFF;
        }

	private synchronized boolean myLevelIsLoggable( int intval )
	{ return ( myLevel == null || intval >= myLevel.intValue() ); }

	private LevelLogger levelLogger( MLevel l )
	{
	    int n = l.intValue();

	    // if a log level has been explicitly set on this logger
	    // and the level we are asked to log at is below this level
	    // don't log anything, i.e. use OffLogger
	    if (! myLevelIsLoggable( n ) ) return offL;

	    if (n >= SEVERE_INTVAL) return errorL;
	    else if (n >= WARNING_INTVAL) return warnL;
	    else if (n >= INFO_INTVAL) return infoL;
	    else if (n >= FINER_INTVAL) return debugL;
	    else if (n >= FINEST_INTVAL) return traceL;
	    else return offL;
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
	    public void log( String msg )                     {}
	    public void log( String format, Object param)     {}
	    public void log( String format, Object[] params)  {}
	    public void log( String msg, Throwable t )        {}
	}

	private class TraceLogger implements LevelLogger
	{
	    public void log( String msg )                     { logger.trace( msg ); }
	    public void log( String format, Object param)     { logger.trace( format, param ); }
	    public void log( String format, Object[] params)  { logger.trace( format, params ); }
	    public void log( String msg, Throwable t )        { logger.trace( msg, t ); }
	}

	private class DebugLogger implements LevelLogger
	{
	    public void log( String msg )                     { logger.debug( msg ); }
	    public void log( String format, Object param)     { logger.debug( format, param ); }
	    public void log( String format, Object[] params)  { logger.debug( format, params ); }
	    public void log( String msg, Throwable t )        { logger.debug( msg, t ); }
	}

	private class InfoLogger implements LevelLogger
	{
	    public void log( String msg )                     { logger.info( msg ); }
	    public void log( String format, Object param)     { logger.info( format, param ); }
	    public void log( String format, Object[] params)  { logger.info( format, params ); }
	    public void log( String msg, Throwable t )        { logger.info( msg, t ); }
	}

	private class WarnLogger implements LevelLogger
	{
	    public void log( String msg )                     { logger.warn( msg ); }
	    public void log( String format, Object param)     { logger.warn( format, param ); }
	    public void log( String format, Object[] params)  { logger.warn( format, params ); }
	    public void log( String msg, Throwable t )        { logger.warn( msg, t ); }
	}
	
	private class ErrorLogger implements LevelLogger
	{
	    public void log( String msg )                     { logger.error( msg ); }
	    public void log( String format, Object param)     { logger.error( format, param ); }
	    public void log( String format, Object[] params)  { logger.error( format, params ); }
	    public void log( String msg, Throwable t )        { logger.error( msg, t ); }
	}

        public ResourceBundle getResourceBundle()
        { return null; }

        public String getResourceBundleName()
        { return null; }

        public void setFilter(Object java14Filter) throws SecurityException
        { warning("setFilter() not supported by MLogger " + this.getClass().getName()); }

        public Object getFilter()
        { return null; }

        public void log(MLevel l, String msg)
	{ levelLogger( l ).log( msg ); }

        public void log(MLevel l, String msg, Object param)
        { levelLogger( l ).log( msg, param ); }

        public void log(MLevel l,String msg, Object[] params)
	{ levelLogger( l ).log( msg, params ); }

        public void log(MLevel l, String msg, Throwable t)
	{ levelLogger( l ).log( msg, t ); }

        public void logp(MLevel l, String srcClass, String srcMeth, String msg)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, msg) ); }

        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param)
        { levelLogger(l).log(  createMessage( srcClass, srcMeth, (msg!=null ? MessageFormat.format(msg, new Object[] {param}) : null) ) ); }

        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, (msg!=null ? MessageFormat.format(msg, params) : null) ) ); }

        public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, msg ),  t); }

        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, formatMessage(rb, msg, null) ) ); }

        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, formatMessage(rb, msg, new Object[] { param } ) ) ); }

        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, formatMessage(rb, msg, params) ) ); }

        public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t)
        { levelLogger(l).log( createMessage( srcClass, srcMeth, formatMessage(rb, msg, null) ),  t); }

        public void entering(String srcClass, String srcMeth)
        { traceL.log( createMessage( srcClass, srcMeth, "entering method." ) ); }

        public void entering(String srcClass, String srcMeth, Object param)
        { traceL.log( createMessage( srcClass, srcMeth, "entering method... param: " + param.toString() ) ); }

        public void entering(String srcClass, String srcMeth, Object params[])
        { traceL.log( createMessage( srcClass, srcMeth, "entering method... " + LogUtils.createParamsList( params ) ) ); }

        public void exiting(String srcClass, String srcMeth)
        { traceL.log( createMessage( srcClass, srcMeth, "exiting method." ) ); }

        public void exiting(String srcClass, String srcMeth, Object result)
        { traceL.log( createMessage( srcClass, srcMeth, "exiting method... result: " + result.toString() ) ); }

        public void throwing(String srcClass, String srcMeth, Throwable t)
        { traceL.log( createMessage( srcClass, srcMeth, "throwing exception... " ),  t); }

        public void severe(String msg)
        { errorL.log( msg ); }

        public void warning(String msg)
        { warnL.log( msg ); }

        public void info(String msg)
        { infoL.log( msg ); }

        public void config(String msg)
        { debugL.log( msg ); }

        public void fine(String msg)
        { debugL.log( msg ); }

        public void finer(String msg)
        { debugL.log( msg ); }

        public void finest(String msg)
        { traceL.log( msg ); }

        public synchronized void setLevel(MLevel l) throws SecurityException
        { myLevel = l; }

        public synchronized MLevel getLevel()
        { 
            if (myLevel == null)
                myLevel = guessMLevel();
            return myLevel;
        }

        public boolean isLoggable(MLevel l)
        { return levelLogger( l ) != offL; }

        public String getName()
        { return logger.getName(); }

        public void addHandler(Object h) throws SecurityException
        { 
	    throw new UnsupportedOperationException("Handlers not supported; the 'handler' " + h + " is not compatible with MLogger " + this); 
        }

        public void removeHandler(Object h) throws SecurityException
        {
	    throw new UnsupportedOperationException("Handlers not supported; the 'handler' " + h + " is not compatible with MLogger " + this); 
        }

        public Object[] getHandlers()
        { return EMPTY_OBJ_ARRAY; }

        public void setUseParentHandlers(boolean uph)
        { throw new UnsupportedOperationException("Handlers not supported."); }

        public boolean getUseParentHandlers()
        { throw new UnsupportedOperationException("Handlers not supported."); }
    }
}
