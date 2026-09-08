package com.mchange.v1.cachedstore;

import java.util.*;

/** 
 * Not thread-safe... use synchronized wrapper in multithreaded
 * situations
 */
class SimpleWritableCachedStore implements WritableCachedStore
{
    private final static Object REMOVE_TOKEN = new Object();

    TweakableCachedStore        readOnlyCache;
    WritableCachedStore.Manager manager;

    HashMap<Object,Object> writeCache = new HashMap<Object,Object>();

    Set<Object> failedWrites = null;

    /** the readOnlyCache MUST use manager for its CachedStore.Manager... */
    SimpleWritableCachedStore( TweakableCachedStore readOnlyCache, 
			       WritableCachedStore.Manager manager)
    {
	this.readOnlyCache = readOnlyCache;
	this.manager = manager;
    }
			       
    @Override
    public Object find(Object key) throws CachedStoreException
    {
	Object out = writeCache.get( key );
	if ( out == null )
	    out = readOnlyCache.find( key ); 
	return (out == REMOVE_TOKEN ? null : out);
    }

    @Override
    public void write(Object key, Object value) 
    { writeCache.put( key, value ); }

    @Override
    public void remove( Object key )
    { write( key, REMOVE_TOKEN ); }

    @Override
    public void flushWrites() throws CacheFlushException
    {
	@SuppressWarnings("unchecked") // HashMap.clone() is declared to return Object
	HashMap<Object,Object> writeCacheCopy = (HashMap<Object,Object>) writeCache.clone();
	for (Iterator<Object> ii = writeCacheCopy.keySet().iterator(); ii.hasNext(); )
	    { 
		Object key = ii.next();
		Object val = writeCacheCopy.get( key );

		try
		    {
			if ( val == REMOVE_TOKEN )
			    manager.removeFromStorage( key );
			else
			    manager.writeToStorage( key, val );
			
			try
			    {
				if (val == REMOVE_TOKEN)
				    readOnlyCache.removeFromCache( key );
				else
				    readOnlyCache.setCachedValue( key, val );
				writeCache.remove( key );
				if (failedWrites != null)
				    {
					failedWrites.remove( key );
					if (failedWrites.size() == 0)
					    failedWrites = null;
				    }
			    }
			catch (CachedStoreException e)
			    { 
				throw new CachedStoreError("SimpleWritableCachedStore:" +
							   " Internal cache is broken!");
			    }
		    }
		catch (Exception e)
		    {
			if (failedWrites == null)
			    failedWrites = new HashSet<Object>();
			failedWrites.add( key );
		    }
	    }

	if (failedWrites != null)
	    throw new CacheFlushException("Some keys failed to write!");
    }

    /** @return an unmodifiable snapshot of current failedWrites set, or null if there have been no failed writes. */
    @Override
    public Set<Object> getFailedWrites()
    { return (failedWrites == null ? null : Collections.unmodifiableSet( new HashSet<Object>(failedWrites) ) ); }

    @Override
    public void clearPendingWrites()
    { 
	writeCache.clear(); 
	failedWrites = null;
    }

    @Override
    public void reset() throws CachedStoreException
    {
	writeCache.clear();
	readOnlyCache.reset();
	failedWrites = null;
    }

    @Override
    public void sync() throws CachedStoreException
    {
	flushWrites();
	reset();
    }
}










