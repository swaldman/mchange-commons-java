package com.mchange.v2.cfg;

import java.util.List;

public class ConfigVetoedException extends ConfigParseException
{
    VetoableConfig source;
    String         identifier;

    public ConfigVetoedException(VetoableConfig source, String identifier, String msg, Throwable cause, List<DelayedLogItem> delayedLogItems)
    {
        super( msg + " [source: " + source + ", identifier: " + identifier + "]", cause, delayedLogItems );
        this.source = source;
        this.identifier = identifier;
    }

    public VetoableConfig getSource()     { return source;     }
    public String         getIdentifier() { return identifier; }
}
