package com.mchange.v2.reflect;

public final class InstantiationNotPermittedException extends Exception
{
    public InstantiationNotPermittedException(String message, Throwable cause)
    { super( message, cause ); }

    public InstantiationNotPermittedException(String message)
    { this( message, null ); }
}
