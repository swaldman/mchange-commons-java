package com.mchange.v2.naming;

public class SecurelyStringifiableConstructionForbiddenException extends SecurelyStringifiableException
{
    public SecurelyStringifiableConstructionForbiddenException( String message, Throwable t )
    { super( message, t ); }

    public SecurelyStringifiableConstructionForbiddenException( String message )
    { super( message ); }

    public SecurelyStringifiableConstructionForbiddenException( Throwable t )
    { super( t ); }

    public SecurelyStringifiableConstructionForbiddenException()
    { super(); }
}
