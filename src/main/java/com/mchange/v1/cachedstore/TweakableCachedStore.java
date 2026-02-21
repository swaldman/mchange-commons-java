package com.mchange.v1.cachedstore;

import java.util.Iterator;

public interface TweakableCachedStore extends CachedStore
{
    /** @return null if the value for this key is not cached */
    public Object getCachedValue(Object key) 
	throws CachedStoreException;

    public void removeFromCache(Object key) 
	throws CachedStoreException;

    public void setCachedValue(Object key, Object value) 
	throws CachedStoreException;

    public Iterator cachedKeys() 
	throws CachedStoreException;
}

