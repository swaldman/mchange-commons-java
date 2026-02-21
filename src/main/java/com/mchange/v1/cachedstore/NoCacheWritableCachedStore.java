package com.mchange.v1.cachedstore;

import java.util.Collections;
import java.util.Set;

class NoCacheWritableCachedStore implements WritableCachedStore, Autoflushing
{
    WritableCachedStore.Manager mgr;

    NoCacheWritableCachedStore(WritableCachedStore.Manager mgr)
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

    public void write(Object key, Object value) throws CachedStoreException
    { 
	try { mgr.writeToStorage( key , value ); }
	catch (Exception e)
	    {
		e.printStackTrace();
		throw CachedStoreUtils.toCachedStoreException( e ); 
	    }
    }

    public void remove(Object key) throws CachedStoreException
    { 
	try { mgr.removeFromStorage( key ); }
	catch (Exception e)
	    {
		e.printStackTrace();
		throw CachedStoreUtils.toCachedStoreException( e ); 
	    }
    }

    public void flushWrites() throws CacheFlushException
    {}

    public Set  getFailedWrites() throws CachedStoreException
    { return Collections.EMPTY_SET; }

    public void clearPendingWrites() throws CachedStoreException
    {}

    public void sync() throws CachedStoreException
    {}
}
