package com.mchange.v1.cachedstore;

import com.mchange.lang.PotentiallySecondaryException;

public class CacheFlushException extends CachedStoreException
{
    public CacheFlushException(String msg, Throwable t)
    {super(msg, t);}

    public CacheFlushException(Throwable t)
    {super(t);}

    public CacheFlushException(String msg)
    {super(msg);}

    public CacheFlushException()
    {super();}
}
