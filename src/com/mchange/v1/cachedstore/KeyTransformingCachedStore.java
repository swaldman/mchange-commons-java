package com.mchange.v1.cachedstore;

import java.util.Iterator;
import com.mchange.v1.util.WrapperIterator;

abstract class KeyTransformingCachedStore extends NoCleanupCachedStore
{
    protected KeyTransformingCachedStore( CachedStore.Manager manager )
    { super( manager ); }

    @Override
    public Object getCachedValue(Object key)
    { return cache.get( toCacheFetchKey( key ) ); }

    @Override
    public void removeFromCache(Object key) 
	throws CachedStoreException
    { cache.remove( toCacheFetchKey( key ) ); }

    @Override
    public void setCachedValue(Object key, Object value) 
	throws CachedStoreException
    {
	//System.err.println("setCachedValue( " + key + " , " + value + " )");
	Object newKey = toCachePutKey( key );
	//System.err.println("put( " + newKey + " , " + value + " )");
	cache.put( newKey , value ); 
    }

    @Override
    public Iterator cachedKeys() throws CachedStoreException
    { 
	return new WrapperIterator( cache.keySet().iterator(), false )
	    {
		@Override
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

