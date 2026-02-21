package com.mchange.v2.csv;

import com.mchange.lang.PotentiallySecondaryException;

public class MalformedCsvException extends PotentiallySecondaryException
{
    public MalformedCsvException(String msg, Throwable t)
    {super(msg, t);}

    public MalformedCsvException(Throwable t)
    {super(t);}

    public MalformedCsvException(String msg)
    {super(msg);}

    public MalformedCsvException()
    {super();}
}
