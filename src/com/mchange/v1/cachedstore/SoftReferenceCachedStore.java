package com.mchange.v1.cachedstore;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * by default, this class is NOT thread safe!!! use the synchronized
 * wrappers defined in CachedStoreUtils for multithreaded situations!
 *
 * @see CachedStoreUtils#synchronizedCachedStore
 * @see CachedStoreUtils#synchronizedTweakableCachedStore
 */
class SoftReferenceCachedStore extends ValueTransformingCachedStore
{
    public SoftReferenceCachedStore(CachedStore.Manager manager)
    { super( manager ); }

    @Override
    protected Object toUserValue( Object cacheValue )
    { return cacheValue == null ? null : ((SoftReference<?>) cacheValue).get(); }

    @Override
    protected Object toCacheValue( Object userValue )
    { return userValue == null ? null : new SoftReference<Object>( userValue ); }

}



