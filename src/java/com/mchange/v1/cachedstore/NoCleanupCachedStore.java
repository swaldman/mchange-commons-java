/*
 * Distributed as part of mchange-commons-java v.0.2.3.2
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

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

class NoCleanupCachedStore implements TweakableCachedStore
{
    final static boolean DEBUG = true;

    protected Map cache = new HashMap();
    
    CachedStore.Manager manager;

    public NoCleanupCachedStore(CachedStore.Manager manager) 
    { this.manager = manager; }

    // be careful if you modify this! subclasses depend upon this
    // exact implementation when overriding methods for key / value
    // transformations!
    public Object find(Object key) throws CachedStoreException
    {
	try
	    {
		Object out = getCachedValue( key );
		if (out == null || manager.isDirty(key, out))
		    {
			out = manager.recreateFromKey(key);
			if (out != null)
			    setCachedValue(key, out);
		    }
		return out;
	    }
	catch (CachedStoreException e)
	    { throw e; }
	catch (Exception e)
	    { 
		if (DEBUG) 
		    e.printStackTrace();
		throw new CachedStoreException(e);
	    }
    }

    //overridden by subclasses!
    public Object getCachedValue(Object key)
    { return cache.get( key ); }

    //overridden by subclasses!
    public void removeFromCache(Object key) 
	throws CachedStoreException
    { cache.remove( key ); }

    //overridden by subclasses!
    public void setCachedValue(Object key, Object value) 
	throws CachedStoreException
    { cache.put(key, value); }

    //overridden by subclasses!
    public Iterator cachedKeys() throws CachedStoreException
    { return cache.keySet().iterator(); }

    public void reset()
    { cache.clear(); }
}



