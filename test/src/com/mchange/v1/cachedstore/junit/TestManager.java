package com.mchange.v1.cachedstore.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mchange.v1.cachedstore.WritableCachedStore;

/**
 *  An inspectable Manager backed by a plain Map, with hooks for marking keys
 *  dirty and for forcing back-end failures. Implements the WritableCachedStore
 *  extension of CachedStore.Manager, so one instance serves both the read-only
 *  and the writable stores.
 */
class TestManager implements WritableCachedStore.Manager
{
    /** stands in for back-end storage; inspect directly to see what was flushed */
    final Map storage = new HashMap();

    /** keys whose cached values isDirty() should report as stale */
    final Set dirtyKeys = new HashSet();

    /** keys for which every back-end operation should throw */
    final Set failKeys = new HashSet();

    /** every 'cached' value isDirty() has been handed, in call order */
    final List isDirtyCachedValues = new ArrayList();

    int recreateCount = 0;
    int isDirtyCount  = 0;
    int writeCount    = 0;
    int removeCount   = 0;

    public boolean isDirty(Object key, Object cached) throws Exception
    {
	++isDirtyCount;
	isDirtyCachedValues.add( cached );
	return dirtyKeys.contains( key );
    }

    public Object recreateFromKey(Object key) throws Exception
    {
	++recreateCount;
	if ( failKeys.contains( key ) )
	    throw new Exception("TestManager: recreateFromKey failed for " + key);
	return storage.get( key );
    }

    public void writeToStorage(Object key, Object value) throws Exception
    {
	++writeCount;
	if ( failKeys.contains( key ) )
	    throw new Exception("TestManager: writeToStorage failed for " + key);
	storage.put( key, value );
    }

    public void removeFromStorage(Object key) throws Exception
    {
	++removeCount;
	if ( failKeys.contains( key ) )
	    throw new Exception("TestManager: removeFromStorage failed for " + key);
	storage.remove( key );
    }
}
