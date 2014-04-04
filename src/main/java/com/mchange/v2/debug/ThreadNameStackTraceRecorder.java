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

package com.mchange.v2.debug;

import java.text.*;
import java.util.*;
import com.mchange.lang.ThrowableUtils;

public class ThreadNameStackTraceRecorder
{
    final static String NL = System.getProperty("line.separator", "\r\n");

    Set set = new HashSet();

    String dumpHeader;
    String stackTraceHeader;

    public ThreadNameStackTraceRecorder( String dumpHeader )
    { this( dumpHeader, "Debug Stack Trace." ); }

    public ThreadNameStackTraceRecorder( String dumpHeader, String stackTraceHeader )
    {
	this.dumpHeader = dumpHeader;
	this.stackTraceHeader = stackTraceHeader;
    }

    public synchronized Object record()
    { 
	Record r = new Record( stackTraceHeader );
	set.add( r );
	return r;
    }

    public synchronized void remove( Object rec )
    { set.remove( rec ); }

    public synchronized int size()
    { return set.size(); }

    public synchronized String getDump()
    { return getDump(null); }

    public synchronized String getDump(String locationSpecificNote)
    {
	DateFormat df = new SimpleDateFormat("dd-MMMM-yyyy HH:mm:ss.SSSS");

	StringBuffer sb = new StringBuffer(2047);
	sb.append(NL);
	sb.append("----------------------------------------------------");
	sb.append(NL);
	sb.append( dumpHeader );
	sb.append(NL);
	if (locationSpecificNote != null)
	    {
		sb.append( locationSpecificNote );
		sb.append( NL );
	    }
	boolean first = true;
	for (Iterator ii = set.iterator(); ii.hasNext(); )
	    {
		if (first) 
		    first = false;
		else 
		    {
			sb.append("---");
			sb.append( NL );
		    }

		Record r = (Record) ii.next();
		sb.append(df.format( new Date( r.time ) ));
		sb.append(" --> Thread Name: ");
		sb.append(r.threadName);
		sb.append(NL);
		sb.append("Stack Trace: ");
		sb.append( ThrowableUtils.extractStackTrace( r.stackTrace ) );
	    }
	sb.append("----------------------------------------------------");
	sb.append(NL);
	return sb.toString();	
    }

    private final static class Record implements Comparable
    {
	long time;
	String threadName;
	Throwable stackTrace;

	Record(String sth)
	{
	    this.time = System.currentTimeMillis();
	    this.threadName = Thread.currentThread().getName();
	    this.stackTrace = new Exception( sth );
	}

	public int compareTo( Object o )
	{
	    Record oo = (Record) o;
	    if ( this.time > oo.time )
		return 1;
	    else if (this.time < oo.time )
		return -1;
	    else
		{
		    int mine = System.identityHashCode( this );
		    int yours = System.identityHashCode( oo );
		    if (mine > yours)
			return 1;
		    else if (mine < yours)
			return -1;
		    return 0;
		}
	}
    }
}
