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

