package com.mchange.v2.cfg;

public class InsecureConfigurationException extends ConfigVetoedException
{
    public InsecureConfigurationException(VetoableConfig source, String identifier, String msg, Throwable cause)
    { super( source, identifier, msg, cause ); }

    public InsecureConfigurationException(VetoableConfig source, String identifier, String msg)
    { this( source, identifier, msg, null ); }
}
