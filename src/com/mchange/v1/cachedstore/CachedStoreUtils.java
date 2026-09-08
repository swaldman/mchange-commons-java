package com.mchange.v1.cachedstore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import com.mchange.lang.PotentiallySecondary;

public final class CachedStoreUtils
{
    final static boolean DEBUG = true;

    public static CachedStore synchronizedCachedStore(final CachedStore orig)
    {
        return new CachedStore()
        {
		@Override
		public synchronized Object find(Object key) throws CachedStoreException
		{ return orig.find( key ); }

		@Override
		public synchronized void reset() throws CachedStoreException
		{ orig.reset(); }
        };
    }

    /**
     *  Note {@code cachedKeys()} returns an Iterator on an unmodifiable <em>snapshot</em> of the current cachedKeys set.
     *  Any calls to {@code remove()} will yield an {@code UnsupportedOperationException}.
     */
    public static TweakableCachedStore synchronizedTweakableCachedStore(final TweakableCachedStore orig)
    {
        return new TweakableCachedStore()
        {
            @Override
            public synchronized Object find(Object key) throws CachedStoreException
            { return orig.find( key ); }

            @Override
            public synchronized void reset() throws CachedStoreException
            { orig.reset(); }

            @Override
            public synchronized Object getCachedValue(Object key) throws CachedStoreException
            { return orig.getCachedValue(key); }

            @Override
            public synchronized void removeFromCache(Object key) throws CachedStoreException
            { orig.removeFromCache(key); }

            @Override
            public synchronized void setCachedValue(Object key, Object value) throws CachedStoreException
            { orig.setCachedValue(key, value); }

            @Override
            public synchronized Iterator<Object> cachedKeys() throws CachedStoreException
            {
                ArrayList<Object> al = new ArrayList<Object>();
                Iterator<Object> csIter = orig.cachedKeys();
                while( csIter.hasNext() ) al.add(csIter.next());
                final Iterator<Object> inner = al.iterator();
                return new Iterator<Object>()
                {
                    @Override
                    public boolean hasNext() { return inner.hasNext(); }
                    @Override
                    public Object  next()    { return inner.next(); }
                    @Override
                    public void remove()
                    { throw new UnsupportedOperationException("Remove not supported by this Iterator."); }
                };
            }
        };
    }

    public static WritableCachedStore synchronizedWritableCachedStore(final WritableCachedStore orig)
    {
        return new WritableCachedStore()
        {
            @Override
            public synchronized Object find(Object key) throws CachedStoreException
            { return orig.find( key ); }

            @Override
            public synchronized void reset() throws CachedStoreException
            { orig.reset(); }

            @Override
            public synchronized void write(Object key, Object value) throws CachedStoreException
            { orig.write(key, value); }

            @Override
            public synchronized void remove(Object key) throws CachedStoreException
            { orig.remove(key); }

            @Override
            public synchronized void flushWrites() throws CacheFlushException
            { orig.flushWrites(); }

            @Override
            public synchronized Set<Object>  getFailedWrites() throws CachedStoreException
            { return orig.getFailedWrites(); }

            @Override
            public synchronized void clearPendingWrites() throws CachedStoreException
            { orig.clearPendingWrites(); }

            @Override
            public synchronized void sync() throws CachedStoreException
            { orig.sync(); }
        };
    }

    public static CachedStore untweakableCachedStore(final TweakableCachedStore orig)
    {
	return new CachedStore()
	    {
		@Override
		public Object find(Object key) throws CachedStoreException
		{ return orig.find( key ); }

		@Override
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
