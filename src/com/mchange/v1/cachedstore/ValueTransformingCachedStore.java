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

