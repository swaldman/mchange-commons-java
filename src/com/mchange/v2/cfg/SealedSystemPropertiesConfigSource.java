package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

final class SealedSystemPropertiesConfigSource implements PropertiesConfigSource
{
    @Override
    public Parse propertiesFromSource( String identifier ) throws Exception
    {
        List<DelayedLogItem> delayedLogItems = new ArrayList<>();
        if ( SealedSystemProperties.NOTIONAL_RESOURCE_PATH.equals( identifier ) )
        {
            Properties props = SealedSystemProperties.initGet(delayedLogItems).getPropertiesByPrefix(""); 
            return new Parse( props, delayedLogItems );
        }
        else
            throw new Exception(  String.format("Unexpected identifier for sealed System properties: '%s'", identifier) );
    }
}

