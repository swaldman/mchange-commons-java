package com.mchange.v2.log;

public class MLogInitializationException extends Exception
{
    public MLogInitializationException( String message )
    { super( message ); }

    public MLogInitializationException( String message, Throwable cause )
    { super( message, cause ); }
}
