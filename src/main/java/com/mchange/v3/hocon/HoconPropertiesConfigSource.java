package com.mchange.v3.hocon;

import java.util.*;
import java.io.*;
import com.typesafe.config.*;

import com.mchange.v2.cfg.*;

import com.mchange.v2.lang.SystemUtils;

import java.net.MalformedURLException;
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
 *  Elements within this path are interpreted as URLs if they contain a colon, e.g.
 *
 *    hocon:reference,application,http://my.host.name/networkconfig,/
 *
 *  URL elements can contain substitutions based on System properties or environment variables
 *  (with System properties taking preference if both are found)
 *
 *    hocon:reference,application,file:${user.home}/.myconfig,/
 *
 *  NOTE: Hash symbols (#) within URLs will be interpreted as denoting a config scope (see below).
 *        They and any subsequent text will NOT be treated as part of the URL
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

			// XXX: This code has been extracted into HoconUtils, but not refactored out.
			//      ( If there is something wrong here, don't forget to check there also
			//        for the same issue. )
			//
			// TODO: Refactor the two classes, maybe using a Callable<String> in the main
			//       utility to deal with heterogeneous failure responses
			if ("application".equals( resourcePath ) || "/application".equals( resourcePath ))
			    {
				// following typesafe-config standard behavior, we override
				// the standard identifier "application" if System properties
				// "config.resource", "config.file", or "config.url" are set.

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
				URL url = null;

				if ( resourcePath.indexOf(":") >= 0 )
				{
				    try
				    {
					String substitutedPath = SystemUtils.sysPropsEnvReplace( resourcePath );
					url = new URL( substitutedPath );
				    }
				    catch ( MalformedURLException e )
				    {
					dlis.add( new DelayedLogItem( Level.WARNING, String.format("Apparent URL resource path for HOCON '%s' could not be parsed as a URL.", resourcePath), e ) );
					// leave URL as null
				    }
				}

				if ( url != null )
				{
				    rawConfig = ConfigFactory.parseURL( url );
				}
				else
				{
				    if (resourcePath.charAt(0) == '/') // when loading resources from a classloader, leave out the leading slash...
					resourcePath = resourcePath.substring(1);
				    
				    boolean includes_suffix = (resourcePath.indexOf('.') >= 0);
				    
				    if ( includes_suffix )
					rawConfig = ConfigFactory.parseResources( cl, resourcePath );
				    else
					rawConfig = ConfigFactory.parseResourcesAnySyntax( cl, resourcePath );
				}
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

