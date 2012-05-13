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

import java.util.Iterator;
import com.mchange.v1.util.WrapperIterator;

abstract class KeyValueTransformingCachedStore extends ValueTransformingCachedStore
{
    protected KeyValueTransformingCachedStore( CachedStore.Manager manager )
    { super( manager ); }

    public Object getCachedValue(Object key)
    { return toUserValue( cache.get( toCacheFetchKey( key ) ) ); }

    public void clearCachedValue(Object key) 
	throws CachedStoreException
    { cache.remove( toCacheFetchKey( key ) ); }

    public void setCachedValue(Object key, Object value) 
	throws CachedStoreException
    { cache.put( toCachePutKey( key ), toCacheValue( value ) ); }

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

    protected Object toCacheFetchKey( Object userKey )
    { return userKey; }

    protected Object toCachePutKey( Object userKey )
    { return userKey; }
}

