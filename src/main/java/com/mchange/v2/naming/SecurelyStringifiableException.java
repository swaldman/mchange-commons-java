package com.mchange.v2.naming;

public class SecurelyStringifiableException extends Exception
{
    public SecurelyStringifiableException( String message, Throwable t )
    { super( message, t ); }

    public SecurelyStringifiableException( String message )
    { super( message ); }

    public SecurelyStringifiableException( Throwable t )
    { super( t ); }

    public SecurelyStringifiableException()
    { super(); }
}
