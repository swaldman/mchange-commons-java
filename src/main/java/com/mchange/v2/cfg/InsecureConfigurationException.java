package com.mchange.v2.cfg;

import java.util.List;

public class InsecureConfigurationException extends ConfigVetoedException
{
    public InsecureConfigurationException(VetoableConfig source, String identifier, String msg, Throwable cause, List<DelayedLogItem> delayedLogItems)
    { super( source, identifier, msg, cause, delayedLogItems ); }
}
