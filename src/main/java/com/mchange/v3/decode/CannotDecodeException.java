package com.mchange.v3.decode;

public class CannotDecodeException extends Exception
{
    public CannotDecodeException( String message, Throwable t )
    { super( message, t ); }

    public CannotDecodeException( String message )
    { super( message ); }

    public CannotDecodeException( Throwable t )
    { super( t ); }

    public CannotDecodeException()
    { super(); }
}

