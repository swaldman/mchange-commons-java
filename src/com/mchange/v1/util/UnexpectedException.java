package com.mchange.v1.util;

import com.mchange.lang.PotentiallySecondaryRuntimeException;

public class UnexpectedException extends PotentiallySecondaryRuntimeException
{
    public UnexpectedException(String msg, Throwable t)
    {super(msg, t);}

    public UnexpectedException(Throwable t)
    {super(t);}

    public UnexpectedException(String msg)
    {super(msg);}

    public UnexpectedException()
    {super();}

    /** @deprecated **/
    public UnexpectedException(Throwable nested, String msg)
    {this(msg, nested);}
}
