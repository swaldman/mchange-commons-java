/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
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

/**
 *  An implementation of {@link com.mchange.v2.cfg.PropertiesConfigSource} that reads HOCON configs 
 *  into properties for incorporation into {@link com.mchange.v2.cfg.MultiPropertiesConfig}.
 *
 *  HOCON config files are read as resources by an identifier, which might look like
 *
 *    hocon:reference,application,special.json,scoped.conf#my-scope,/
 *
 *  All Configs found are merged, with later elements in the list taking preference over
 *  earlier elements. Substitutions within the full, merged Config are resolved.
 * 
 *  Elements that have no suffix (or, more exactly, that contain no '.' character) are read
 *  appending all three HOCON standard suffixes, so that "xxx" reads all three of 
 *  xxx.conf, xxx.json, and xxx.properties.
 *
 *  Anything that follows a '#' character in an identifier is treated as a scope,
 *  such that the only Config loaded (as the new top-level!) are those underneath
 *  the scope key. So, in JSON format, if the config under scoped.conf is
 *
 *  {
 *     "some-top-level-key" : "hello",
 *
 *     "my-scope" : {
 *        "a" : "apple",
 *        "b" : "book",
 *        "c" : "cat"
 *     }
 *  }
 *
 *  scoped.conf#my-scope will be read as:
 *
 *  {
 *     "a" : "apple",
 *     "b" : "book",
 *     "c" : "cat"
 *  }
 *
 *  The special element '/' refers to a Config containing System properties. 
 *
 *  Following HOCON config conventions, resources under the identifier 'application'
 *  will be replaced if any of "config.resource", "config.file", or "config.url"
 *  are set.
 *
 */
public final class HoconPropertiesConfigSource implements PropertiesConfigSource
{
    private static Config extractConfig( ClassLoader cl, String identifier, List<DelayedLogItem> dlis ) throws FileNotFoundException, Exception
    {
	int pfx_index = identifier.indexOf(':');

	List <Config> configs = new ArrayList<Config>();

	if ( pfx_index >= 0 && "hocon".equals( identifier.substring(0, pfx_index).toLowerCase() ) )
	{
	    String allFilesStr = identifier.substring( pfx_index + 1 ).trim();
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
			Config rawConfig = null;
			
			// following typesafe-config standard behavior, we override
			// the standard identifier "application" if System properties
			// "config.resource", "config.file", or "config.url" are set.
			if ("application".equals( resourcePath ) || "/application".equals( resourcePath ))
			    {
				String check; 
				if ( ( check = System.getProperty("config.resource") ) != null )
				    resourcePath = check;
				else if ( ( check = System.getProperty("config.file") ) != null )
				    {
					File f = new File( check );
					if ( f.exists() )
					    {
						if ( f.canRead() )
						    rawConfig = ConfigFactory.parseFile( f );
						else
						    dlis.add( new DelayedLogItem( Level.WARNING, 
										  String.format("Specified config.file '%s' is not readable. Falling back to standard application.(conf|json|properties).}", f.getAbsolutePath()) ) );
					    }
					else
					    dlis.add( new DelayedLogItem( Level.WARNING, 
									  String.format("Specified config.file '%s' does not exist. Falling back to standard application.(conf|json|properties).}", f.getAbsolutePath()) ) );

				    }
				else if ( ( check = System.getProperty("config.url") ) != null )
				    rawConfig = ConfigFactory.parseURL( new URL( check ) );
			    }

			if ( rawConfig == null )
			    {		
				if (resourcePath.charAt(0) == '/') // when loading resources from a classloader, leave out the leading slash...
				    resourcePath = resourcePath.substring(1);
				
				boolean includes_suffix = (resourcePath.indexOf('.') >= 0);
				
				if ( includes_suffix )
				    rawConfig = ConfigFactory.parseResources( cl, resourcePath );
				else
				    rawConfig = ConfigFactory.parseResourcesAnySyntax( cl, resourcePath );
			    }
				
			if ( rawConfig.isEmpty() )
			    dlis.add( new DelayedLogItem( Level.FINE, String.format("Missing or empty HOCON configuration for resource path '%s'.", resourcePath) ) );
			else
			    config = rawConfig;
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
		    return bigConfig.resolve(); // impose intra-Config substitutions
		}
	}
	else
	    throw new IllegalArgumentException( String.format("Invalid resource identifier for hocon config file: '%s'", identifier) );
    }
    
    public Parse propertiesFromSource( ClassLoader cl, String identifier ) throws FileNotFoundException, Exception
    {
	List<DelayedLogItem> dlis = new LinkedList<DelayedLogItem>();
	
	Config config = extractConfig( cl, identifier, dlis );
	PropertiesConversion pc = configToProperties( config );
	
	for( String path : pc.unrenderable )
	    dlis.add( new DelayedLogItem( Level.FINE, String.format("Value at path '%s' could not be converted to a String. Skipping.", path) ) );
	
	return new Parse( pc.properties, dlis );
    }

    public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception
    { return propertiesFromSource( HoconPropertiesConfigSource.class.getClassLoader(), identifier ); }
}

