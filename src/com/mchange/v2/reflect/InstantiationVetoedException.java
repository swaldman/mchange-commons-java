package com.mchange.v2.reflect;

public final class InstantiationVetoedException extends Exception
{
    public InstantiationVetoedException(String message, Throwable cause)
    { super( message, cause ); }

    public InstantiationVetoedException(String message)
    { this( message, null ); }
}
