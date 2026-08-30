package com.mchange.v1.cachedstore;

public interface Vacuumable
{
    public void vacuum() throws CachedStoreException;
}
