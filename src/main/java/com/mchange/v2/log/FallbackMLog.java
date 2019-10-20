/*
 * Distributed as part of mchange-commons-java 0.2.11
 *
 * Copyright (C) 2015 Machinery For Change, Inc.
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

import java.text.*;
import java.util.*;
import java.util.logging.*;
import com.mchange.lang.ThrowableUtils;
import com.mchange.v2.log.jdk14logging.Jdk14LoggingUtils;

public final class FallbackMLog extends MLog
{
    final static MLevel DEFAULT_CUTOFF_LEVEL;
    final static String SEP;

    // MT: protected by this' lock
    private static MLevel overrideCutoffLevel = null;
    private static Filter globalFilter        = null;

    // MT: unchanging, internally thread-safe
    private final MLogger nameless = new FallbackMLogger();

    static
    {
	MLevel dflt = null;
	String dfltName = MLogConfig.getProperty( "com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL" );
	if (dfltName != null)
	    dflt = MLevel.fromSeverity( dfltName );
	if (dflt == null)
	    dflt = MLevel.INFO;
	DEFAULT_CUTOFF_LEVEL = dflt;

	SEP = System.getProperty( "line.separator" );
    }

    public synchronized MLevel cutoffLevel()
    {
	if ( overrideCutoffLevel != null )
	    return overrideCutoffLevel;
	else
	    return DEFAULT_CUTOFF_LEVEL;
    }

    public interface Filter {
	boolean isLoggable( MLevel level, String loggerName, String srcClass, String srcMeth, String msg, Object[] params, Throwable t );
    }

    private static class Jdk14FilterAdapter implements Filter
    {
	private java.util.logging.Filter julFilter;
	
	Jdk14FilterAdapter( java.util.logging.Filter julFilter )
	{ this.julFilter = julFilter; }

	java.util.logging.Filter getInner()
	{ return julFilter; }
	
	public boolean isLoggable( MLevel level, String loggerName, String srcClass, String srcMeth, String msg, Object[] params, Throwable t )
	{
	    LogRecord lr = new LogRecord( Jdk14LoggingUtils.levelFromMLevel( level ), msg );
	    lr.setLoggerName( loggerName );
	    lr.setSourceClassName( srcClass );
	    lr.setSourceMethodName( srcMeth );
	    lr.setParameters( params );
	    lr.setThrown(t);
	    return julFilter.isLoggable(lr);
	}
    }

    /** @deprecated use setOverrideCutoffLevel(...) */
    public void overrideCutoffLevel( MLevel level )
    { this.setOverrideCutoffLevel( level ); }
	
    public synchronized void setOverrideCutoffLevel( MLevel level )
    { overrideCutoffLevel = level; }

    public synchronized MLevel getOverrideCutoffLevel()
    { return overrideCutoffLevel; }

    private static Filter filterFromObject( Object o )
    {
	if ( o instanceof Filter ) return (Filter) o;
	else if ( o instanceof java.util.logging.Filter) return new Jdk14FilterAdapter( (java.util.logging.Filter) o );
	else throw new IllegalArgumentException( "Provided filter " + o + " must be either a FallbackMLog.Filter or an instance of java.util.logging.Filter." );
    }

    public synchronized void setGlobalFilter( Object filter )
    { this.globalFilter = filterFromObject( filter ); }

    public synchronized Object getGlobalFilter()
    {
	if (this.globalFilter instanceof Jdk14FilterAdapter) return ((Jdk14FilterAdapter) this.globalFilter).getInner();
	else return this.globalFilter;
    }

    private synchronized Filter _getGlobalFilter()
    { return this.globalFilter; }

    public MLogger getMLogger(String name)
    { return new FallbackMLogger(name); } 

    public MLogger getMLogger()
    { return nameless; }

    private final class FallbackMLogger implements MLogger
    {
	// MT: constant post construction
	String name;

	// MT: protected by this' lock
	Filter filter = null;

	public FallbackMLogger(String name)
	{ this.name = name; }

	public FallbackMLogger()
	{ this.name = null; }
	
	private void formatrb(MLevel l, String srcClass, String srcMeth, String rbname, String msg, Object[] params, Throwable t)
	{
	    ResourceBundle rb = ResourceBundle.getBundle( rbname );
	    if (msg != null && rb != null)
		{
		    String check = rb.getString( msg );
		    if (check != null)
			msg = check;
		}
	    format( l, srcClass, srcMeth, msg, params, t);
	}

	private boolean isLoggableMessage(MLevel l, String srcClass, String srcMeth, String msg, Object[] params, Throwable t)
	{
	    // System.err.println( "isLoggableMessage( " + l + ", " + srcClass + ", " + srcMeth + ", " + msg + ", " + params + ", " + t + " ) -- loggerName: " + this.name);
	    
	    Filter globalFilter = _getGlobalFilter();
	    boolean globalOkay = globalFilter == null || globalFilter.isLoggable( l, this.name, srcClass, srcMeth, msg, params, t );
	    if ( globalOkay )
	    {
		Filter localFilter = this._getFilter();
		boolean localOkay  = localFilter == null || localFilter.isLoggable( l, this.name, srcClass, srcMeth, msg, params, t );
		return localOkay;
	    }
	    else return false;
	}

	private void format(MLevel l, String srcClass, String srcMeth, String msg, Object[] params, Throwable t)
	{
	    if ( isLoggableMessage( l, srcClass, srcMeth, msg, params, t ) )
		System.err.println( formatString( l, srcClass, srcMeth, msg, params, t ) );
	}

	private String formatString(MLevel l, String srcClass, String srcMeth, String msg, Object[] params, Throwable t)
	{
	    boolean add_parens = (srcMeth != null && ! srcMeth.endsWith(")"));
		
	    StringBuffer sb = new StringBuffer(256);
	    sb.append(l.getLineHeader());
	    sb.append(' ');
	    if ( name != null )
	    {
		sb.append( name );
		sb.append(' ');
	    }
	    if (srcClass != null && srcMeth != null)
		{
		    sb.append('[');
		    sb.append( srcClass );
		    sb.append( '.' );
		    sb.append( srcMeth );
		    if (add_parens)
			sb.append("()");
		    sb.append( ']' );
		}
	    else if (srcClass != null)
		{
		    sb.append('[');
		    sb.append( srcClass );
		    sb.append( ']' );
		}
	    else if (srcMeth != null)
		{
		    sb.append('[');
		    sb.append( srcMeth );
		    if (add_parens)
			sb.append("()");
		    sb.append( ']' );
		}
	    if (msg == null) 
		{
		    if (params != null)
			{
			    sb.append("params: ");
			    for (int i = 0, len = params.length; i < len; ++i)
				{
				    if (i != 0) sb.append(", ");
				    sb.append( params[i] );
				}
			}
		}
	    else 
		{
		    if (params == null)
			sb.append( msg );
		    else
			{
			    MessageFormat mfmt = new MessageFormat( msg );
			    sb.append( mfmt.format( params ) );
			}
		}
	    
	    if (t != null)
	    {
		sb.append( SEP );
		sb.append( ThrowableUtils.extractStackTrace( t ) );
	    }

	    return sb.toString();
	}

	public ResourceBundle getResourceBundle()
	{
	    //warn("Using logger " + this.getClass().getName() + ", which does not support ResourceBundles.");
	    return null;
	}

	public String getResourceBundleName()
	{ return null; }

	public synchronized void setFilter( Object filter )
	{ this.filter = filterFromObject( filter ); }
	
	public synchronized Object getFilter()
	{
	    if (this.filter instanceof Jdk14FilterAdapter) return ((Jdk14FilterAdapter) this.filter).getInner();
	    else return this.filter;
	}

	private synchronized Filter _getFilter()
	{ return this.filter; }

	public void log(MLevel l, String msg)
	{ 
	    if ( isLoggable( l ) )
		format( l, null, null, msg, null, null ); 
	}

	public void log(MLevel l, String msg, Object param)
	{ 
	    if ( isLoggable( l ) )
		format( l, null, null, msg, new Object[] { param }, null ); 
	}

	public void log(MLevel l,String msg, Object[] params)
	{ 
	    if ( isLoggable( l ) )
		format( l, null, null, msg, params, null ); 
	}

	public void log(MLevel l, String msg, Throwable t)
	{ 
	    if ( isLoggable( l ) )
		format( l, null, null, msg, null, t ); 
	}

	public void logp(MLevel l, String srcClass, String srcMeth, String msg)
	{ 
	    if ( isLoggable( l ) )
		format( l, srcClass, srcMeth, msg, null, null ); 
	}

	public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object param)
	{ 
	    if ( isLoggable( l ) )
		format( l, srcClass, srcMeth, msg, new Object[] { param }, null ); 
	}

	public void logp(MLevel l, String srcClass, String srcMeth, String msg, Object[] params)
	{ 
	    if ( isLoggable( l ) )
		format( l, srcClass, srcMeth, msg, params, null ); 
	}

	public void logp(MLevel l, String srcClass, String srcMeth, String msg, Throwable t)
	{ 
	    if ( isLoggable( l ) )
		format( l, srcClass, srcMeth, msg, null, t ); 
	}

	public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg)
	{ 
	    if ( isLoggable( l ) )
		formatrb( l, srcClass, srcMeth, rb, msg, null, null ); 
	}

	public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object param)
	{ 
	    if ( isLoggable( l ) )
		formatrb( l, srcClass, srcMeth, rb, msg, new Object[] { param }, null ); 
	}

	public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Object[] params)
	{ 
	    if ( isLoggable( l ) )
		formatrb( l, srcClass, srcMeth, rb, msg, params, null ); 
	}

	public void logrb(MLevel l, String srcClass, String srcMeth, String rb, String msg, Throwable t)
	{ 
	    if ( isLoggable( l ) )
		formatrb( l, srcClass, srcMeth, rb, msg, null, t ); 
	}

	public void entering(String srcClass, String srcMeth)
	{ 
	    if ( isLoggable( MLevel.FINER ) )
		format(MLevel.FINER, srcClass, srcMeth, "Entering method.", null, null); 
	}

	public void entering(String srcClass, String srcMeth, Object param)
	{ 
	    if ( isLoggable( MLevel.FINER ) )
		format(MLevel.FINER, srcClass, srcMeth, "Entering method with argument " + param, null, null); 
	}

	public void entering(String srcClass, String srcMeth, Object[] params)
	{ 
	    if ( isLoggable( MLevel.FINER ) )
		{
		    if (params == null)
			entering( srcClass, srcMeth );
		    else
			{
			    StringBuffer sb = new StringBuffer(128);
			    sb.append("( ");
			    for (int i = 0, len = params.length; i < len; ++i)
				{
				    if (i != 0) sb.append(", ");
				    sb.append( params[i] );
				}
			    sb.append(" )");
			    format(MLevel.FINER, srcClass, srcMeth, "Entering method with arguments " + sb.toString(), null, null); 
			}
		}
	}

	public void exiting(String srcClass, String srcMeth)
	{ 
	    if ( isLoggable( MLevel.FINER ) )
		format(MLevel.FINER, srcClass, srcMeth, "Exiting method.", null, null); 
	}

	public void exiting(String srcClass, String srcMeth, Object result)
	{ 
	    if ( isLoggable( MLevel.FINER ) )
		format(MLevel.FINER, srcClass, srcMeth, "Exiting method with result " + result, null, null); 
	}

	public void throwing(String srcClass, String srcMeth, Throwable t)
	{ 
	    if ( isLoggable( MLevel.FINE ) )
		format(MLevel.FINE, srcClass, srcMeth, "Throwing exception." , null, t); 
	}

	public void severe(String msg)
	{ 
	    if ( isLoggable( MLevel.SEVERE ) )
		format(MLevel.SEVERE, null, null, msg, null, null); 
	}

	public void warning(String msg)
	{ 
	    if ( isLoggable( MLevel.WARNING ) )
		format(MLevel.WARNING, null, null, msg, null, null); 
	}

	public void info(String msg)
	{ 
	    if ( isLoggable( MLevel.INFO ) )
		format(MLevel.INFO, null, null, msg, null, null); 
	}

	public void config(String msg)
	{ 
	    if ( isLoggable( MLevel.CONFIG ) )
		format(MLevel.CONFIG, null, null, msg, null, null); 
	}

	public void fine(String msg)
	{ 
	    if ( isLoggable( MLevel.FINE ) )
		format(MLevel.FINE, null, null, msg, null, null); 
	}

	public void finer(String msg)
	{ 
	    if ( isLoggable( MLevel.FINER ) )
		format(MLevel.FINER, null, null, msg, null, null); 
	}

	public void finest(String msg)
	{ 
	    if ( isLoggable( MLevel.FINEST ) )
		format(MLevel.FINEST, null, null, msg, null, null); 
	}

	public void setLevel(MLevel l) throws SecurityException
	{ overrideCutoffLevel( l ); }
					      
	public synchronized MLevel getLevel()
	{ return cutoffLevel(); }

	public synchronized boolean isLoggable(MLevel l)
	{ return (l.intValue() >= cutoffLevel().intValue()); }

	public String getName()
	{ return "global"; }

	public void addHandler(Object h) throws SecurityException
	{ 
	    warning("Using FallbackMLog -- Handlers not supported."); 
	}

	public void removeHandler(Object h) throws SecurityException
	{
	    warning("Using FallbackMLog -- Handlers not supported.");
	}

	public Object[] getHandlers()
	{ 
	    warning("Using FallbackMLog -- Handlers not supported.");
	    return new Object[0];
	}

	public void setUseParentHandlers(boolean uph)
	{ 
	    warning("Using FallbackMLog -- Handlers not supported.");
	}

	public boolean getUseParentHandlers()
	{ return false;	}
    }
}
