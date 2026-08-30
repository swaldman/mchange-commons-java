package com.mchange.v1.cachedstore;

import java.lang.ref.ReferenceQueue;

class ManualCleanupSoftKeyCachedStore extends KeyTransformingCachedStore implements Vacuumable
{
    ReferenceQueue queue = new ReferenceQueue();

    public ManualCleanupSoftKeyCachedStore(CachedStore.Manager manager)
    { super( manager ); }

    protected Object toUserKey( Object cachePutKey )
    { return ((SoftKey) cachePutKey).get(); }

    protected Object toCacheFetchKey( Object userKey )
    { return new SoftKey( userKey, null ); }

    protected Object toCachePutKey( Object userKey )
    { return new SoftKey( userKey, queue ); }

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


