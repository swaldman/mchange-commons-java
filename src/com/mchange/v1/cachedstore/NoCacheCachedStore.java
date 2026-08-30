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
