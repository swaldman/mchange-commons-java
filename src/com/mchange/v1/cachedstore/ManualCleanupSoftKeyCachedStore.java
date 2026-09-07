package com.mchange.v1.cachedstore;

import java.lang.ref.ReferenceQueue;

class ManualCleanupSoftKeyCachedStore extends KeyTransformingCachedStore implements Vacuumable
{
    ReferenceQueue queue = new ReferenceQueue();

    public ManualCleanupSoftKeyCachedStore(CachedStore.Manager manager)
    { super( manager ); }

    @Override
    protected Object toUserKey( Object cachePutKey )
    { return ((SoftKey) cachePutKey).get(); }

    @Override
    protected Object toCacheFetchKey( Object userKey )
    { return new SoftKey( userKey, null ); }

    @Override
    protected Object toCachePutKey( Object userKey )
    { return new SoftKey( userKey, queue ); }

    @Override
    public void vacuum() throws CachedStoreException
    { 
	SoftKey key;
	while ((key = (SoftKey) queue.poll()) != null)
	    {
		//System.err.println("Vacuuming Key ---> " + key);
		this.removeByTransformedKey( key );
	    }
    }
}


