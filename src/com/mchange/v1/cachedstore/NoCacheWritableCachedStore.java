package com.mchange.v1.cachedstore;

import java.util.Set;

class NoCacheWritableCachedStore implements WritableCachedStore, Autoflushing
{
    WritableCachedStore.Manager mgr;

    NoCacheWritableCachedStore(WritableCachedStore.Manager mgr)
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
    public void write(Object key, Object value) throws CachedStoreException
    { 
	try { mgr.writeToStorage( key , value ); }
	catch (Exception e)
	    {
		e.printStackTrace();
		throw CachedStoreUtils.toCachedStoreException( e ); 
	    }
    }

    @Override
    public void remove(Object key) throws CachedStoreException
    { 
	try { mgr.removeFromStorage( key ); }
	catch (Exception e)
	    {
		e.printStackTrace();
		throw CachedStoreUtils.toCachedStoreException( e ); 
	    }
    }

    @Override
    public void flushWrites() throws CacheFlushException
    {}

    @Override
    public Set  getFailedWrites() throws CachedStoreException
    { return null; }

    @Override
    public void clearPendingWrites() throws CachedStoreException
    {}

    @Override
    public void sync() throws CachedStoreException
    {}
}
