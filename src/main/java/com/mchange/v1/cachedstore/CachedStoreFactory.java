package com.mchange.v1.cachedstore;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;

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
	InvocationHandler handler = new InvocationHandler()
	    {
		public Object invoke(Object proxy, Method m, Object[] args) 
		    throws Throwable
		{
		    inner.vacuum();
		    return m.invoke( inner, args ); 
		}
	    };
	return (TweakableCachedStore) Proxy.newProxyInstance( CachedStoreFactory.class.getClassLoader(),
							      new Class[] { TweakableCachedStore.class },
							      handler );
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

