package com.mchange.v1.cachedstore;

import java.util.Iterator;
import java.util.Set;
import com.mchange.lang.PotentiallySecondary;
import com.mchange.v1.lang.Synchronizer;

public final class CachedStoreUtils
{
    final static boolean DEBUG = true;

    public static CachedStore synchronizedCachedStore(final CachedStore orig)
    {
        return new CachedStore()
        {
		public synchronized Object find(Object key) throws CachedStoreException
		{ return orig.find( key ); }

		public synchronized void reset() throws CachedStoreException
		{ orig.reset(); }
        };
    }

    public static TweakableCachedStore synchronizedTweakableCachedStore(final TweakableCachedStore orig)
    {
        return new TweakableCachedStore()
        {
            public synchronized Object find(Object key) throws CachedStoreException
            { return orig.find( key ); }

            public synchronized void reset() throws CachedStoreException
            { orig.reset(); }

            /** @return null if the value for this key is not cached */
            public synchronized Object getCachedValue(Object key) throws CachedStoreException
            { return orig.getCachedValue(key); }

            public synchronized void removeFromCache(Object key) throws CachedStoreException
            { orig.removeFromCache(key); }

            public synchronized void setCachedValue(Object key, Object value) throws CachedStoreException
            { orig.setCachedValue(key, value); }

            public synchronized Iterator cachedKeys() throws CachedStoreException
            { return orig.cachedKeys(); }
        };
    }

    public static WritableCachedStore synchronizedWritableCachedStore(final WritableCachedStore orig)
    {
        return new WritableCachedStore()
        {
            public synchronized Object find(Object key) throws CachedStoreException
            { return orig.find( key ); }

            public synchronized void reset() throws CachedStoreException
            { orig.reset(); }

            public synchronized void write(Object key, Object value) throws CachedStoreException
            { orig.write(key, value); }

            public synchronized void remove(Object key) throws CachedStoreException
            { orig.remove(key); }

            public synchronized void flushWrites() throws CacheFlushException
            { orig.flushWrites(); }

            public synchronized Set  getFailedWrites() throws CachedStoreException
            { return orig.getFailedWrites(); }

            public synchronized void clearPendingWrites() throws CachedStoreException
            { orig.clearPendingWrites(); }

            public synchronized void sync() throws CachedStoreException
            { orig.sync(); }
        };
    }

    public static CachedStore untweakableCachedStore(final TweakableCachedStore orig)
    {
	return new CachedStore()
	    {
		public Object find(Object key) throws CachedStoreException
		{ return orig.find( key ); }

		public void reset() throws CachedStoreException
		{ orig.reset(); }
	    };
    }

    static CachedStoreException toCachedStoreException( Throwable t )
    {
	if (DEBUG) t.printStackTrace();

	if (t instanceof CachedStoreException)
	    return (CachedStoreException) t;
	else if (t instanceof PotentiallySecondary)
	    {
		Throwable t2 = ((PotentiallySecondary) t).getNestedThrowable();
		if (t2 instanceof CachedStoreException)
		    return (CachedStoreException) t2;
	    }
	return new CachedStoreException( t );
	    
    }

    static CacheFlushException toCacheFlushException( Throwable t )
    {
	if (DEBUG) t.printStackTrace();

	if (t instanceof CacheFlushException)
	    return (CacheFlushException) t;
	else 
	    return new CacheFlushException( t );
	    
    }

    private CachedStoreUtils()
    {}
}
