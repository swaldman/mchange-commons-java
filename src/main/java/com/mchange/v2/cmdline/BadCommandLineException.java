package com.mchange.v2.cmdline;

public class BadCommandLineException extends Exception
{
    public BadCommandLineException(String msg)
    { super(msg); }

    public BadCommandLineException()
    { super(); }
}
