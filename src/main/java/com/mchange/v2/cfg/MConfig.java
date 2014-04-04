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

package com.mchange.v2.cfg;

import java.util.*;
import com.mchange.v2.log.*;
import com.mchange.v1.cachedstore.*;
import com.mchange.v1.util.ArrayUtils;

public final class MConfig
{
    private final static MLogger logger = MLog.getLogger( MConfig.class );

    private final static Map<DelayedLogItem.Level,MLevel> levelMap;

    static
    {
	try
	{
	    Map<DelayedLogItem.Level,MLevel> lm = new HashMap();
	    for( DelayedLogItem.Level level : DelayedLogItem.Level.values() )
		lm.put( level, (MLevel) (MLevel.class.getField( level.toString() ).get( null )) );
	    levelMap = Collections.unmodifiableMap( lm );
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

    final static CachedStore cache = CachedStoreUtils.synchronizedCachedStore( CachedStoreFactory.createNoCleanupCachedStore( new CSManager() ) );

    public static MultiPropertiesConfig readVmConfig(String[] defaults, String[] preempts)
    {
	try
	{ return (MultiPropertiesConfig) cache.find( new PathsKey( defaults, preempts ) ); }
	catch (CachedStoreException e)
	{ throw new RuntimeException( e ); }
    }

    public static MultiPropertiesConfig readVmConfig()
    { return readVmConfig( ConfigUtils.NO_PATHS, ConfigUtils.NO_PATHS ); }

    public static MultiPropertiesConfig readConfig( String[] resourcePaths )
    { 
	try
	{ return (MultiPropertiesConfig) cache.find( new PathsKey( resourcePaths ) ); }
	catch (CachedStoreException e)
	{ throw new RuntimeException( e ); }
    }

    /**
     *  Later entries in the configs array override earlier entries.
     */
    public static MultiPropertiesConfig combine( MultiPropertiesConfig[] configs )
    { return ConfigUtils.combine( configs ); }

    public static void dumpToLogger(List<DelayedLogItem> items, MLogger logger)
    { for( DelayedLogItem item : items ) dumpToLogger( item, logger ); }

    public static void dumpToLogger( DelayedLogItem item, MLogger logger )
    { logger.log( levelMap.get( item.getLevel() ), item.getText(), item.getException() ); }

    private final static class PathsKey
    {
	String[] paths;
	List     delayedLogItems;
	
	public boolean equals(Object o)
	{ 
	    if (o instanceof PathsKey)
		return Arrays.equals( paths, ((PathsKey) o).paths );
	    else
		return false;
	}

	public int hashCode()
	{ return ArrayUtils.hashArray( paths ); }

	PathsKey(String[] defaults, String[] preempts) // will include resource-configured resource paths
	{
	    this.delayedLogItems = new ArrayList();

	    List pathList = ConfigUtils.vmCondensedPaths( defaults, preempts, delayedLogItems );
	    this.paths = (String[]) pathList.toArray( new String[ pathList.size() ] );
	}

	PathsKey(String[] paths)
	{ 
	    this.delayedLogItems = Collections.emptyList();
	    this.paths = paths; 
	}
    }

    private static class CSManager implements CachedStore.Manager
    {
	public boolean isDirty(Object key, Object cached) throws Exception
	{ return false; }

	public Object recreateFromKey(Object key) throws Exception
	{
	    PathsKey pk = (PathsKey) key;

	    /*
	    for( Iterator ii = pk.delayedLogItems.iterator(); ii.hasNext(); )
	    {
		DelayedLogItem pm = (DelayedLogItem) ii.next();
		logger.log( pm.getLevel(), pm.getText(), pm.getException() );
	    }
	    */

	    List<DelayedLogItem> items = new ArrayList<DelayedLogItem>();
	    items.addAll( pk.delayedLogItems );
	    Object out =  ConfigUtils.read( pk.paths, items );
	    dumpToLogger( items, logger );
	    return out;
	}
    }

    private MConfig()
    {}
}
