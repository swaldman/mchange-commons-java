package com.mchange.v1.util;

import com.mchange.lang.PotentiallySecondaryException;

public class UnreliableIteratorException extends PotentiallySecondaryException
{
    public UnreliableIteratorException(String msg, Throwable t)
    {super(msg, t);}

    public UnreliableIteratorException(Throwable t)
    {super(t);}

    public UnreliableIteratorException(String msg)
    {super(msg);}

    public UnreliableIteratorException()
    {super();}
}
