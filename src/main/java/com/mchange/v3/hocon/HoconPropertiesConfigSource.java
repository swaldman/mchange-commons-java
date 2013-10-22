/*
 * Distributed as part of mchange-commons-java 0.2.6.2
 *
 * Copyright (C) 2013 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

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
    private static Config extractConfig( String identifier, List<DelayedLogItem> dlis ) throws FileNotFoundException, Exception
    {
	int pfx_index = identifier.indexOf(':');

	List <Config> configs = new ArrayList<Config>();

	if ( pfx_index >= 0 && "hocon".equals( identifier.substring(0, pfx_index).toLowerCase() ) )
	{
	    String allFilesStr = identifier.substring( pfx_index ).trim();
	    String[] allFiles = allFilesStr.split("\\s*,\\s*");

	    for ( String file : allFiles ) 
	    {
		String resourcePath;
		String scopePath;
		
		int sfx_index = file.lastIndexOf('#');
		if ( sfx_index > 0 )
		    {
			resourcePath = file.substring( 0, sfx_index );
			scopePath = file.substring( sfx_index + 1 ).replace('/','.').trim();
		    }
		else
		    {
			resourcePath = file;
			scopePath = null;
		    }
		
		Config config = null;
		
		if ( "/".equals( resourcePath ) )
		    config = ConfigFactory.systemProperties();
		else
		    {
			URL u = HoconPropertiesConfigSource.class.getResource( resourcePath );
			if ( u == null )
			    dlis.add( new DelayedLogItem( Level.FINE, String.format("Could not find HOCON configuration for resource path '%s'.", resourcePath) ) );
			else
			    config = ConfigFactory.parseURL( u );
		    }
		
		if (config != null) 
		{
		    if (scopePath != null)
			config = config.getConfig( scopePath );
		    
		    configs.add( config );
		}
	    }

	    if ( configs.size() == 0)
		throw new FileNotFoundException( String.format("Could not find HOCON configuration at any of the listed resources in '%s'", identifier) );
	    else
		{
		    Config bigConfig = ConfigFactory.empty();
		    for (int i = configs.size(); --i >= 0; )
			bigConfig = bigConfig.withFallback( configs.get(i) );
		    return bigConfig.resolve();
		}
	}
	else
	    throw new IllegalArgumentException( String.format("Invalid resource identifier for hocon config file: '%s'", identifier) );
    }

    public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception
    {
	List<DelayedLogItem> dlis = new LinkedList<DelayedLogItem>();

	Config config = extractConfig( identifier, dlis );
	PropertiesConversion pc = configToProperties( config );

	for( String path : pc.unrenderable )
	    dlis.add( new DelayedLogItem( Level.FINE, String.format("Value at path '%s' could not be converted to a String. Skipping.", path) ) );

	return new Parse( pc.properties, dlis );
    }
}

