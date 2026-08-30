package com.mchange.v1.cachedstore;

import com.mchange.lang.PotentiallySecondaryError;

public class CachedStoreError extends PotentiallySecondaryError
{
    public CachedStoreError(String msg, Throwable t)
    {super(msg, t);}

    public CachedStoreError(Throwable t)
    {super(t);}

    public CachedStoreError(String msg)
    {super(msg);}

    public CachedStoreError()
    {super();}
}
