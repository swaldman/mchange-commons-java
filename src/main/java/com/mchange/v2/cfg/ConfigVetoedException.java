package com.mchange.v2.cfg;

public class ConfigVetoedException extends Exception
{
    public ConfigVetoedException(String msg, Throwable cause)
    { super( msg, cause ); }

    public ConfigVetoedException(String msg)
    { this( msg, null ); }
}
