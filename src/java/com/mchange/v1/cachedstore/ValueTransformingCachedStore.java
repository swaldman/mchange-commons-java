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

abstract class ValueTransformingCachedStore extends NoCleanupCachedStore
{
    protected ValueTransformingCachedStore( CachedStore.Manager manager )
    { super( manager ); }

    public Object getCachedValue(Object key)
    { return toUserValue( cache.get( key ) ); }

    public void removeFromCache(Object key) 
	throws CachedStoreException
    { cache.remove( key ); }

    public void setCachedValue(Object key, Object value) 
	throws CachedStoreException
    { cache.put( key , toCacheValue( value ) ); }

    protected Object toUserValue( Object cacheValue )
    { return cacheValue; }

    protected Object toCacheValue( Object userValue )
    { return userValue; }
}

