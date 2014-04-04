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


import java.util.*;
import java.lang.reflect.Method;
import com.mchange.v2.cfg.MLogConfigSource;
import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.MConfig;

public final class MLogConfig
{
    // MT: all now mutable references, protected by class' lock
    private static MultiPropertiesConfig CONFIG              = null;
    private static List                  BOOTSTRAP_LOG_ITEMS = null;
    private static Method                delayedDumpToLogger = null;

    public synchronized static void refresh( MultiPropertiesConfig[] overrides, String overridesDescription )
    {
	String[] defaults = new String[] { "/com/mchange/v2/log/default-mchange-log.properties"  };
	String[] preempts = new String[] { "/mchange-log.properties", "/" };

	List bli = new ArrayList();
	MultiPropertiesConfig tmpCONFIG = MLogConfigSource.readVmConfig( defaults, preempts, bli );

	boolean firstLoad = (CONFIG == null);

	if ( overrides != null )
	{
	    int olen = overrides.length;
	    MultiPropertiesConfig[] combineMe = new MultiPropertiesConfig[ olen + 1 ];
	    combineMe[0] = tmpCONFIG;
	    for ( int i = 0; i < olen; ++i )
		combineMe[ i + 1 ] = overrides[i];
	    bli.add( new DelayedLogItem( DelayedLogItem.Level.INFO, (firstLoad ? "Loaded" : "Refreshed") + " MLog library log configuration, with overrides" + (overridesDescription == null ? "." : ": " + overridesDescription) ) );
	    CONFIG = MConfig.combine( combineMe );
	}
	else
	{
	    if ( !firstLoad )
		bli.add( new DelayedLogItem( DelayedLogItem.Level.INFO, "Refreshed MLog library log configuration, without overrides.") );
	    CONFIG = tmpCONFIG;
	}
	BOOTSTRAP_LOG_ITEMS = bli;
    }

    // should be called only from static synchronized methods
    private static void ensureLoad()
    { if (CONFIG == null) refresh( null, null); }

    // should be called only from static synchronized methods
    private static void ensureDelayedDumpToLogger()
    {
	try
	{
	    if ( delayedDumpToLogger == null )
	    {
		Class mConfigClass = Class.forName( "com.mchange.v2.cfg.MConfig" );
		Class delayedLogItemClass = Class.forName( "com.mchange.v2.cfg.DelayedLogItem" );
		delayedDumpToLogger = mConfigClass.getMethod("dumpToLogger", new Class[] { delayedLogItemClass, MLogger.class } );
	    }
	}
	catch ( RuntimeException e )
	{ 
	    e.printStackTrace();
	    throw e; 
	}
	catch ( Exception e )
	{ 
	    e.printStackTrace();
	    throw new RuntimeException( e ); 
	}
    }

    public synchronized static String getProperty( String key )
    {
	ensureLoad();
	return CONFIG.getProperty( key ); 
    }

    // should not be called during static init to avoid cyclic dependency issues
    public synchronized static void logDelayedItems( MLogger logger )
    { 
	ensureLoad();
	ensureDelayedDumpToLogger();

	List items = new ArrayList();
	items.addAll( BOOTSTRAP_LOG_ITEMS );
	items.addAll( CONFIG.getDelayedLogItems() );

	Set uniquerizer = new HashSet();
	uniquerizer.addAll( items );
	
	for( Iterator ii = items.iterator(); ii.hasNext(); )
	{
	    Object item = ii.next();

	    if (uniquerizer.contains( item ) )
	    {
		uniquerizer.remove( item );

		try { delayedDumpToLogger.invoke( null, new Object[] { item, logger } ); }
		catch ( Exception e )
		    {
			// bad, bad, shouldn't happen
			e.printStackTrace();
			throw new Error(e);
		    }
	    }
	}
    }

    public synchronized static String dump()
    { return CONFIG.toString(); }

    private MLogConfig()
    {}
}
