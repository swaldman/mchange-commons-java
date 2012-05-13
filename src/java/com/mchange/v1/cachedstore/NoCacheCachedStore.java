/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
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

import java.util.Collections;
import java.util.Iterator;
import com.mchange.v1.util.IteratorUtils;

class NoCacheCachedStore implements TweakableCachedStore
{
    CachedStore.Manager mgr;

    NoCacheCachedStore(CachedStore.Manager mgr)
    { this.mgr = mgr; }

    public Object find(Object key) throws CachedStoreException
    { 
	try {return mgr.recreateFromKey( key ); }
	catch (Exception e)
	    {
		e.printStackTrace();
		throw CachedStoreUtils.toCachedStoreException( e ); 
	    }
    }

    public void reset()
    {}

    public Object getCachedValue(Object key) 
    { return null; }

    public void removeFromCache(Object key) 
    {}

    public void setCachedValue(Object key, Object value) 
    {}

    public Iterator cachedKeys() 
    { return IteratorUtils.EMPTY_ITERATOR; }
}
