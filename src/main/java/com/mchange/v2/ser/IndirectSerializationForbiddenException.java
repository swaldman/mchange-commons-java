package com.mchange.v2.ser;

import java.io.IOException;

public class IndirectSerializationForbiddenException extends IOException
{
    public IndirectSerializationForbiddenException( String message, Throwable cause )
    { super( message, cause ); }

    public IndirectSerializationForbiddenException( String message )
    { this( message, null ); }
}
