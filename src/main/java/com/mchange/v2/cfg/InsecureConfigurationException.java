package com.mchange.v2.cfg;

public class InsecureConfigurationException extends ConfigVetoedException
{
    public InsecureConfigurationException(String msg, Throwable cause)
    { super( msg, cause ); }

    public InsecureConfigurationException(String msg)
    { this( msg, null ); }
}
