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



