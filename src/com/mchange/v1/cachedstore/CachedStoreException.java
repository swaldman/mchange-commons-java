package com.mchange.v1.cachedstore;

public class CachedStoreException extends Exception
{
    public CachedStoreException(String msg, Throwable t)
    {super(msg, t);}

    public CachedStoreException(Throwable t)
    {super(t);}

    public CachedStoreException(String msg)
    {super(msg);}

    public CachedStoreException()
    {super();}
}
