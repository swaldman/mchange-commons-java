/*
 * Distributed as part of mchange-commons-java v.0.2.4
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

import java.util.Iterator;
import com.mchange.v1.util.WrapperIterator;

abstract class KeyTransformingCachedStore extends NoCleanupCachedStore
{
    protected KeyTransformingCachedStore( CachedStore.Manager manager )
    { super( manager ); }

    public Object getCachedValue(Object key)
    { return cache.get( toCacheFetchKey( key ) ); }

    public void removeFromCache(Object key) 
	throws CachedStoreException
    { cache.remove( toCacheFetchKey( key ) ); }

    public void setCachedValue(Object key, Object value) 
	throws CachedStoreException
    {
	//System.err.println("setCachedValue( " + key + " , " + value + " )");
	Object newKey = toCachePutKey( key );
	//System.err.println("put( " + newKey + " , " + value + " )");
	cache.put( newKey , value ); 
    }

    public Iterator cachedKeys() throws CachedStoreException
    { 
	return new WrapperIterator( cache.keySet().iterator(), false )
	    {
		public Object transformObject( Object o )
		{
		    Object out = toUserKey( o );
		    return ( out == null ? SKIP_TOKEN : out );
		}
	    };
    }

    protected Object toUserKey( Object cachePutKey )
    { return cachePutKey; }

    /** 
     * @return the key that will be used for gets and removes to the 
     * inner HashMap
     */
    protected Object toCacheFetchKey( Object userKey )
    { return toCachePutKey( userKey ); }

    /** 
     * @return the key that will be used for puts into the 
     * inner HashMap
     */
    protected Object toCachePutKey( Object userKey )
    { return userKey; }

    protected Object removeByTransformedKey( Object cacheFetchKey )
    { return cache.remove( cacheFetchKey ); }
}

