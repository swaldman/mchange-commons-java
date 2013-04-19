package com.mchange.v3.hocon;

import java.util.*;
import java.io.*;
import com.typesafe.config.*;

import com.mchange.v2.cfg.*;

import java.net.URL;

import static com.mchange.v3.hocon.HoconUtils.*;
import static com.mchange.v2.cfg.DelayedLogItem.*;

public final class HoconPropertiesConfigSource implements PropertiesConfigSource
{
    private static Config extractConfig( String identifier ) throws FileNotFoundException, Exception
    {
	int pfx_index = identifier.indexOf(':');
	if ( pfx_index >= 0 && "hocon".equals( identifier.substring(0, pfx_index).toLowerCase() ) )
	{
	    String resourcePath;
	    String scopePath;

	    int sfx_index = identifier.lastIndexOf('#');
	    if ( sfx_index > 0 )
		{
		    resourcePath = identifier.substring( pfx_index + 1, sfx_index ).trim();
		    scopePath = identifier.substring( sfx_index + 1 ).replace('/','.').trim();
		}
	    else
		{
		    resourcePath = identifier.substring( pfx_index + 1 );
		    scopePath = null;
		}

	    Config config;

	    if ( "/".equals( resourcePath ) || "".equals( resourcePath ) )
		config = ConfigFactory.load();
	    else
		{
		    URL u = HoconPropertiesConfigSource.class.getResource( resourcePath );
		    if ( u == null )
			throw new FileNotFoundException( String.format("Could not find HOCON configuration for resource path '%s'.", resourcePath) );
		    else
			config = ConfigFactory.parseURL( u );
		}
	    
	    if (scopePath != null)
		config = config.getConfig( scopePath );

	    return config;
	}
	else
	    throw new IllegalArgumentException( String.format("Invalid resource identifier for hocon config file: '%s'", identifier) );
    }

    public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception
    {
	Config config = extractConfig( identifier );
	PropertiesConversion pc = configToProperties( config );

	List<DelayedLogItem> pms = new LinkedList<DelayedLogItem>();
	for( String path : pc.unrenderable )
	    pms.add( new DelayedLogItem( Level.FINE, String.format("Value at path '%s' could not be converted to a String.", path) ) );

	return new Parse( pc.properties, pms );
    }
}

