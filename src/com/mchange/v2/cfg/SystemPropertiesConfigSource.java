package com.mchange.v2.cfg;

import java.util.Collections;
import java.util.Properties;

final class SystemPropertiesConfigSource implements PropertiesConfigSource
{
    @Override
    public Parse propertiesFromSource( String identifier ) throws Exception
    {
        if ( "/".equals( identifier ) )
            return new Parse( (Properties) System.getProperties().clone(), Collections.<DelayedLogItem>emptyList() );
        else
            throw new Exception(  String.format("Unexpected identifier for System properties: '%s'", identifier) );
    }
}

