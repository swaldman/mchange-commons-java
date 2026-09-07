package com.mchange.v1.cachedstore;

import java.util.Collections;
import java.util.Iterator;
import com.mchange.v1.util.IteratorUtils;

class NoCacheCachedStore implements TweakableCachedStore
{
    CachedStore.Manager mgr;

    NoCacheCachedStore(CachedStore.Manager mgr)
    { this.mgr = mgr; }

    @Override
    public Object find(Object key) throws CachedStoreException
    { 
	try {return mgr.recreateFromKey( key ); }
	catch (Exception e)
	    {
		e.printStackTrace();
		throw CachedStoreUtils.toCachedStoreException( e ); 
	    }
    }

    @Override
    public void reset()
    {}

    @Override
    public Object getCachedValue(Object key) 
    { return null; }

    @Override
    public void removeFromCache(Object key) 
    {}

    @Override
    public void setCachedValue(Object key, Object value) 
    {}

    @Override
    public Iterator cachedKeys() 
    { return IteratorUtils.EMPTY_ITERATOR; }
}
