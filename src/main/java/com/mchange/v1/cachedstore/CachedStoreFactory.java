package com.mchange.v1.cachedstore;

import java.util.Iterator;

/**
 * Unless otherwise noted, implementations are to be presumed
 * NOT thread safe!!! use the synchronized
 * wrappers defined in CachedStoreUtils for multithreaded situations!
 *
 * @see CachedStoreUtils#synchronizedCachedStore
 * @see CachedStoreUtils#synchronizedTweakableCachedStore
 */
public final class CachedStoreFactory
{
    public static TweakableCachedStore createNoCleanupCachedStore(CachedStore.Manager manager)
    { return new NoCleanupCachedStore( manager ); }

    /**
     *  Better to use createSynchronousCleanupSoftKeyCachedStore to prevent the build of 
     *  cleaned SoftReferences, unless the universe of potential cacheables is small.
     */
    public static TweakableCachedStore createSoftValueCachedStore(CachedStore.Manager manager)
    { return new SoftReferenceCachedStore( manager ); }

    public static TweakableCachedStore createSynchronousCleanupSoftKeyCachedStore(CachedStore.Manager manager)
    {
	final ManualCleanupSoftKeyCachedStore inner = new ManualCleanupSoftKeyCachedStore( manager );
        return new TweakableCachedStore()
        {
            public Object find(Object key) throws CachedStoreException
            {
                inner.vacuum();
                return inner.find( key );
            }

            public void reset() throws CachedStoreException
            {
                inner.vacuum();
                inner.reset();
            }

            public Object getCachedValue(Object key) throws CachedStoreException
            {
                inner.vacuum();
                return inner.getCachedValue(key);
            }

            public void removeFromCache(Object key) throws CachedStoreException
            {
                inner.vacuum();
                inner.removeFromCache(key);
            }

            public void setCachedValue(Object key, Object value) throws CachedStoreException
            {
                inner.vacuum();
                inner.setCachedValue(key, value);
            }

            public Iterator cachedKeys() throws CachedStoreException
            {
                inner.vacuum();
                return inner.cachedKeys();
            }
        };
    }

    /**
     *  Always reads directly from manager, as if values are always dirty.
     */
    public static TweakableCachedStore createNoCacheCachedStore( CachedStore.Manager mgr )
    { return new NoCacheCachedStore( mgr ); }

    /**
     * creates a WritableCachedStore implementation that uses soft keys and synchronous
     * cleanup for its read cache.
     */
    public static WritableCachedStore createDefaultWritableCachedStore(WritableCachedStore.Manager manager)
    {
	TweakableCachedStore readOnlyCache = createSynchronousCleanupSoftKeyCachedStore( manager );
	return new SimpleWritableCachedStore( readOnlyCache, manager );
    }

    public static WritableCachedStore cacheWritesOnlyWritableCachedStore( WritableCachedStore.Manager mgr )
    { 
	TweakableCachedStore readOnlyCache = createNoCacheCachedStore( mgr );
	return new SimpleWritableCachedStore( readOnlyCache, mgr );
    }

    /**
     *  Always reads directly from manager, as if values are always dirty. Always
     *  writes to back-end storage. Effectively Autoflushing, since every write
     *  is flushed.
     *
     *  @see Autoflushing 
     */
    public static WritableCachedStore createNoCacheWritableCachedStore( WritableCachedStore.Manager mgr )
    { return new NoCacheWritableCachedStore( mgr ); }
}

