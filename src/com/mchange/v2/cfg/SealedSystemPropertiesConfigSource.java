package com.mchange.v2.cfg;

import java.util.Collections;
import java.util.Properties;

final class SealedSystemPropertiesConfigSource implements PropertiesConfigSource
{
    @Override
    public Parse propertiesFromSource( String identifier ) throws Exception
    {
        if ( SealedSystemProperties.NOTIONAL_RESOURCE_PATH.equals( identifier ) )
            return new Parse( SealedSystemProperties.get().getPropertiesByPrefix(""), Collections.<DelayedLogItem>emptyList() );
        else
            throw new Exception(  String.format("Unexpected identifier for sealed System properties: '%s'", identifier) );
    }
}

