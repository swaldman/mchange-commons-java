/*
 * Distributed as part of mchange-commons-java v.0.2.3
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v1.cachedstore;

import java.util.*;

/** 
 * Not thread-safe... use synchronized wrapper in multithreaded
 * situations
 */
class SimpleWritableCachedStore implements WritableCachedStore
{
    private final static Object REMOVE_TOKEN = new Object();

    TweakableCachedStore        readOnlyCache;
    WritableCachedStore.Manager manager;

    HashMap writeCache = new HashMap();

    Set failedWrites = null;

    /** the readOnlyCache MUST use manager for its CachedStore.Manager... */
    SimpleWritableCachedStore( TweakableCachedStore readOnlyCache, 
			       WritableCachedStore.Manager manager)
    {
	this.readOnlyCache = readOnlyCache;
	this.manager = manager;
    }
			       
    public Object find(Object key) throws CachedStoreException
    {
	Object out = writeCache.get( key );
	if ( out == null )
	    out = readOnlyCache.find( key ); 
	return (out == REMOVE_TOKEN ? null : out);
    }

    public void write(Object key, Object value) 
    { writeCache.put( key, value ); }

    public void remove( Object key )
    { write( key, REMOVE_TOKEN ); }

    public void flushWrites() throws CacheFlushException
    {
	HashMap writeCacheCopy = (HashMap) writeCache.clone();
	for (Iterator ii = writeCacheCopy.keySet().iterator(); ii.hasNext(); )
	    { 
		Object key = ii.next();
		Object val = writeCacheCopy.get( key );

		try
		    {
			if ( val == REMOVE_TOKEN )
			    manager.removeFromStorage( key );
			else
			    manager.writeToStorage( key, val );
			
			try
			    {
				readOnlyCache.setCachedValue( key, val );
				writeCache.remove( key );
				if (failedWrites != null)
				    {
					failedWrites.remove( key );
					if (failedWrites.size() == 0)
					    failedWrites = null;
				    }
			    }
			catch (CachedStoreException e)
			    { 
				throw new CachedStoreError("SimpleWritableCachedStore:" +
							   " Internal cache is broken!");
			    }
		    }
		catch (Exception e)
		    {
			if (failedWrites == null)
			    failedWrites = new HashSet();
			failedWrites.add( key );
		    }
	    }

	if (failedWrites != null)
	    throw new CacheFlushException("Some keys failed to write!");
    }

    public Set getFailedWrites()
    { return (failedWrites == null ? null : Collections.unmodifiableSet( failedWrites ) ); }

    public void clearPendingWrites()
    { 
	writeCache.clear(); 
	failedWrites = null;
    }

    public void reset() throws CachedStoreException
    {
	writeCache.clear();
	readOnlyCache.reset();
	failedWrites = null;
    }

    public void sync() throws CachedStoreException
    {
	flushWrites();
	reset();
    }
}










