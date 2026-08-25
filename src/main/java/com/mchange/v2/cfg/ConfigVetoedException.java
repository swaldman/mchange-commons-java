package com.mchange.v2.cfg;

public class ConfigVetoedException extends Exception
{
    VetoableConfig source;
    String         identifier;

    public ConfigVetoedException(VetoableConfig source, String identifier, String msg, Throwable cause)
    {
        super( msg + " [source: " + source + ", identifier: " + identifier + "]", cause );
        this.source = source;
        this.identifier = identifier;
    }

    public ConfigVetoedException(VetoableConfig source, String identifier, String msg)
    { this( source, identifier, msg, null ); }

    public VetoableConfig getSource()     { return source;     }
    public String         getIdentifier() { return identifier; }
}
