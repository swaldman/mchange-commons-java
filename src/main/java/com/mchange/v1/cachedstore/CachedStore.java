package com.mchange.v1.cachedstore;

public interface CachedStore
{
    public Object find(Object key) throws CachedStoreException;

    /** clears any cached values. subsequent finds will recreate from key. */
    public void reset() throws CachedStoreException;

    public interface Manager
    {
	public boolean isDirty(Object key, Object cached) throws Exception;
	public Object recreateFromKey(Object key) throws Exception;
    }
}
