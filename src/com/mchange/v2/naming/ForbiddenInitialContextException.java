package com.mchange.v2.naming;

public class ForbiddenInitialContextException extends Exception
{
    public ForbiddenInitialContextException(String message, Throwable cause)
    { super( message, cause ); }

    public ForbiddenInitialContextException(String message)
    { this( message, null ); }
}
