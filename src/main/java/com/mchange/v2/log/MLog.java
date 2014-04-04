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

package com.mchange.v2.log;

import java.util.List;
import java.util.ArrayList;
import com.mchange.v1.util.StringTokenizerUtils;
import com.mchange.v2.cfg.MultiPropertiesConfig;

public abstract class MLog
{
    // MT: Protected by MLog.class' lock
    private static NameTransformer _transformer;
    private static MLog _mlog;
    private static MLogger _logger;

    static
    { refreshConfig( null, null ); }

    private static synchronized NameTransformer transformer() { return _transformer; }
    private static synchronized MLog            mlog()        { return _mlog; }
    private static synchronized MLogger         logger()      { return _logger; }

    public static synchronized void refreshConfig( MultiPropertiesConfig[] overrides, String overridesDescription )
    {
	MLogConfig.refresh( overrides, overridesDescription );

	String classnamesStr = MLogConfig.getProperty("com.mchange.v2.log.MLog");
	String[] classnames = null;
	if (classnamesStr == null)
	    classnamesStr = MLogConfig.getProperty("com.mchange.v2.log.mlog");
	if (classnamesStr != null)
	    classnames = StringTokenizerUtils.tokenizeToArray( classnamesStr, ", \t\r\n" );

	boolean warn = false;
	MLog tmpml = null;
	if (classnames != null)
	    tmpml = findByClassnames( classnames, true );
	if (tmpml == null)
	    tmpml = findByClassnames( MLogClasses.SEARCH_CLASSNAMES, false );
	if (tmpml == null)
	    {
		warn = true;
		tmpml = new FallbackMLog();
	    }
	_mlog = tmpml;
	if (warn)
	    info("Using " + _mlog.getClass().getName() + " -- Named logger's not supported, everything goes to System.err.");

	NameTransformer tmpt = null;
	String tClassName = MLogConfig.getProperty("com.mchange.v2.log.NameTransformer");
	if (tClassName == null)
	    tClassName = MLogConfig.getProperty("com.mchange.v2.log.nametransformer");
	try
	    { 
		if (tClassName != null)
		    tmpt = (NameTransformer) Class.forName( tClassName ).newInstance();
	    }
	catch ( Exception e )
	    {
		System.err.println("Failed to instantiate com.mchange.v2.log.NameTransformer '" + tClassName + "'!"); 
		e.printStackTrace();
	    }
	_transformer = tmpt;

	_logger = _mlog.getLogger( MLog.class );

	// at this point we are initialized; except for the initilaizer, what follows is essentially client code
	// which we run in a throwaway Thread not holding the class' / classloading lock
	
	Thread bannerThread = new Thread("MLog-Init-Reporter")
	    {
		final MLogger logo;
		String  loggerDesc;

		{
		    logo       = _logger;
		    loggerDesc = _mlog.getClass().getName();
		}

		public void run()
		{
		    if ("com.mchange.v2.log.jdk14logging.Jdk14MLog".equals( loggerDesc ))
			loggerDesc = "java 1.4+ standard";
		    else if ("com.mchange.v2.log.log4j.Log4jMLog".equals( loggerDesc ))
			loggerDesc = "log4j";
		    else if ("com.mchange.v2.log.slf4j.Slf4jMLog".equals( loggerDesc ))
			loggerDesc = "slf4j";
	
		    if (logo.isLoggable( MLevel.INFO ))
			logo.log( MLevel.INFO, "MLog clients using " + loggerDesc + " logging.");

		    //System.err.println(mlog);

		    MLogConfig.logDelayedItems( logo );
	
		    if ( logo.isLoggable( MLevel.FINEST ) )
			logo.log( MLevel.FINEST, "Config available to MLog library: " + MLogConfig.dump() );
		}
	    };
	bannerThread.start();
    }

    // does not require statics to be initialized
    public static MLog findByClassnames( String[] classnames, boolean log_attempts_to_stderr )
    {
	List attempts = null;
	for (int i = 0, len = classnames.length; i < len; ++i)
	    {
		try { return (MLog) Class.forName( MLogClasses.resolveIfAlias( classnames[i] ) ).newInstance(); }
		catch (Exception e)
		    { 
			if (attempts == null)
			    attempts = new ArrayList();
			attempts.add( classnames[i] );
			if ( log_attempts_to_stderr )
			{
			    System.err.println("com.mchange.v2.log.MLog '" + classnames[i] + "' could not be loaded!"); 
			    e.printStackTrace();
			}
		    }
	    }
	System.err.println("Tried without success to load the following MLog classes:");
	for (int i = 0, len = attempts.size(); i < len; ++i)
	    System.err.println("\t" + attempts.get(i));
	return null;
    }

    public static MLog instance()
    { return mlog(); }

    public static MLogger getLogger(String name) 
    {
	NameTransformer xformer = null;
	MLog            insty   = null;

	synchronized ( MLog.class )
	{
	    xformer = transformer();
	    insty = instance();
	}

	MLogger out;
	if ( xformer == null )
	    out = instance().getMLogger( name );
	else
	    {
		String xname = xformer.transformName( name );
		if (xname != null)
		    out = insty.getMLogger( xname );
		else
		    out = insty.getMLogger( name );
	    }
	return out;
    }

    public static MLogger getLogger(Class cl)
    {
	NameTransformer xformer = null;
	MLog            insty   = null;

	synchronized ( MLog.class )
	{
	    xformer = transformer();
	    insty = instance();
	}

	MLogger out;
	if ( xformer == null )
	    out = insty.getMLogger( cl );
	else
	    {
		String xname = xformer.transformName( cl );
		if (xname != null)
		    out = insty.getMLogger( xname );
		else
		    out = insty.getMLogger( cl );
	    }
	return out;
    }

    public static MLogger getLogger()
    {
	NameTransformer xformer = null;
	MLog            insty   = null;

	synchronized ( MLog.class )
	{
	    xformer = transformer();
	    insty = instance();
	}

	MLogger out;
	if ( xformer == null )
	    out = insty.getMLogger();
	else
	    {
		String xname = xformer.transformName();
		if (xname != null)
		    out = insty.getMLogger( xname );
		else
		    out = insty.getMLogger();
	    }
	return out;
    }

    public static void log(MLevel l, String msg)
    { instance().getLogger().log( l, msg ); }

    public static void log(MLevel l, String msg, Object param)
    { instance().getLogger().log( l, msg, param ); }

    public static void log(MLevel l,String msg, Object[] params)
    { instance().getLogger().log( l, msg, params ); }

    public static void log(MLevel l, String msg,Throwable t)
    { instance().getLogger().log( l, msg, t ); }

    public static void logp(MLevel l, String srcClass, String srcMeth, String msg)
    { instance().getLogger().logp( l, srcClass, srcMeth, msg ); }

    public static void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param)
    { instance().getLogger().logp( l, srcClass, srcMeth, msg, param ); }

    public static void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params)
    { instance().getLogger().logp( l, srcClass, srcMeth, msg, params ); }

    public static void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t)
    { instance().getLogger().logp( l, srcClass, srcMeth, msg, t ); }

    public static void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg)
    { instance().getLogger().logp( l, srcClass, srcMeth, rb, msg ); }

    public static void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param)
    { instance().getLogger().logrb( l, srcClass, srcMeth, rb, msg, param ); }

    public static void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params)
    { instance().getLogger().logrb( l, srcClass, srcMeth, rb, msg, params ); }

    public static void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t)
    { instance().getLogger().logrb( l, srcClass, srcMeth, rb, msg, t ); }

    public static void entering(String srcClass, String srcMeth)
    { instance().getLogger().entering( srcClass, srcMeth ); }

    public static void entering(String srcClass, String srcMeth, Object param)
    { instance().getLogger().entering( srcClass, srcMeth, param ); }

    public static void entering(String srcClass, String srcMeth, Object params[])
    { instance().getLogger().entering( srcClass, srcMeth, params ); }

    public static void exiting(String srcClass, String srcMeth)
    { instance().getLogger().exiting( srcClass, srcMeth ); }

    public static void exiting(String srcClass, String srcMeth, Object result)
    { instance().getLogger().exiting( srcClass, srcMeth, result ); }

    public static void throwing(String srcClass, String srcMeth, Throwable t)
    { instance().getLogger().throwing( srcClass, srcMeth, t); }

    public static void severe(String msg)
    { instance().getLogger().severe( msg ); }

    public static void warning(String msg)
    { instance().getLogger().warning( msg ); }

    public static void info(String msg)
    { instance().getLogger().info( msg ); }

    public static void config(String msg)
    { instance().getLogger().config( msg ); }

    public static void fine(String msg)
    { instance().getLogger().fine( msg ); }

    public static void finer(String msg)
    { instance().getLogger().finer( msg ); }

    public static void finest(String msg)
    { instance().getLogger().finest( msg ); }

    // convenience implementation, may be overridden
    public MLogger getMLogger(Class cl)
    { return getMLogger( cl.getName() ); }

    public abstract MLogger getMLogger(String name);
    public abstract MLogger getMLogger();
}
