package com.mchange.v3.hocon;

import java.util.*;
import com.mchange.v2.cfg.*;
import com.typesafe.config.*;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

import static com.typesafe.config.ConfigFactory.*;

public final class HoconUtils
{
    private final static String APPLICATION = "application";

    public static class PropertiesConversion
    {
	Properties  properties;
	Set<String> unrenderable;
    }

    public static PropertiesConversion configToProperties( Config config )
    {
	Set<Map.Entry<String,ConfigValue>> entries = config.entrySet();

	Properties  properties = new Properties();
	Set<String> unrenderable = new HashSet<String>();

	for( Map.Entry<String,ConfigValue> entry : entries )
	{
	    String path = entry.getKey();
	    String value = null;
	    try
	    { value = config.getString( path ); }
	    catch( ConfigException.WrongType e )
	    { unrenderable.add( path ); }

	    if ( value != null )
		properties.setProperty( path, value );
	}

	PropertiesConversion out = new PropertiesConversion();
	out.properties = properties;
	out.unrenderable = unrenderable;
	return out;
    }

    //
    // XXX: The following three methods are reworked from HoconPropertiesConfigSource,
    // which should be refactored to make use of these methods BUT HAS NOT YET.
    //
    // If something is wrong here, please remember to check whether it is not wrong 
    // there too.
    //
    //
    // TODO: Refactor the two classes, maybe using a Callable<String> in the main
    //       utility to deal with heterogeneous failure responses

    /**
     *  For when you don't want all the extras of ConfigFactory.load() [ backing up to reference.conf,
     *  System property overrides, etc. ]
     */
    public static Config applicationOrStandardSubstitute(ClassLoader cl) throws SubstituteNotAvailableException {
	String resourcePath = APPLICATION;

	Config out = null;
	
	// following typesafe-config standard behavior, we override
	// the standard identifier "application" if System properties
	// "config.resource", "config.file", or "config.url" are set.

	String check; 
	if ( ( check = System.getProperty("config.resource") ) != null ) {
	    resourcePath = check;
	} else if ( ( check = System.getProperty("config.file") ) != null ) {
	    File f = new File( check );
	    if ( f.exists() ) {
		if ( f.canRead() ) out = ConfigFactory.parseFile( f );
		else throw new SubstituteNotAvailableException(  String.format( "config.file '%s' (specified as a System property) is not readable.", f.getAbsolutePath() ) );
	    }
	    else { 
		throw new SubstituteNotAvailableException( String.format("Specified config.file '%s' (specified as a System property) does not exist.", f.getAbsolutePath()) ); 
	    }
	} else if ( ( check = System.getProperty("config.url") ) != null ) {
	    try { out = ConfigFactory.parseURL( new URL( check ) ); }
	    catch ( MalformedURLException e ) {
		throw new SubstituteNotAvailableException( String.format("Specified config.url '%s' (specified as a System property) could not be parsed.", check ) ); 
	    }
	}
	if ( out == null ) out = ConfigFactory.parseResourcesAnySyntax( cl, resourcePath );
	return out;
    }

    public static ConfigWithFallbackMessage applicationOrStandardSubstituteFallbackWithMessage(ClassLoader cl) throws SubstituteNotAvailableException {
	try { return new ConfigWithFallbackMessage( applicationOrStandardSubstitute( cl ), null ); }
	catch ( SubstituteNotAvailableException e )
	    { return new ConfigWithFallbackMessage( ConfigFactory.parseResourcesAnySyntax( cl, APPLICATION ), e.getMessage() + " Falling back to standard application.(conf|json|properties)." ); }
    }

    /*
    // Best not to reference the MLogger / MLevel from this package, as those depend upon static initializations 
    // from cfg package stuff that in turn depends upon this package.

    public static ConfigWithFallbackMessage applicationOrStandardSubstituteFallbackWithLogging(ClassLoader cl, MLogger logger, MLevel level) throws SubstituteNotAvailableException {
	try { return applicationOrStandardSubstitute( cl ); }
	catch ( SubstituteNotAvailableException e )
	    { 
		if ( logger.isLoggable( level ) )
		    logger.log( e.getMessage() + " Falling back to standard application.(conf|json|properties)." );
		return ConfigFactory.parseResourcesAnySyntax( cl, APPLICATION );
	    }
    }
    */

    public static class SubstituteNotAvailableException extends Exception {
	SubstituteNotAvailableException( String msg ) { 
	    super( msg ); 
	}
    }
    public static class ConfigWithFallbackMessage {
	private Config _config;
	private String _message; 

	public Config config() { return _config; }
	public String message() { return _message; }

	private ConfigWithFallbackMessage( Config config, String message ) {
	    this._config = config;
	    this._message = message;
	}
    }

    public static class WarnedConfig
    {
	public Config       config;
	public List<String> warnings;

	WarnedConfig( Config config, List<String> warnings )
	{
	    this.config   = config;
	    this.warnings = warnings;
	}
    }

    /*
     * This preserves the standard application / resource / etc behavior,
     * but puts a custom config before application (but still after System properties), 
     * unless at least one of the standard config.resource / config.file / config.url 
     * System.properties are set.
     */
    public static WarnedConfig customFileOrSpecifiedSourceWins( File customFile ) {
	List<String> warnings = new ArrayList<String>();

	boolean    fileExists         = customFile.exists();
	Properties sysprops           = System.getProperties();
	boolean    configuredLocation = sysprops.containsKey( "config.resource" ) || sysprops.containsKey( "config.file" ) || sysprops.containsKey( "config.url" );

	if ( configuredLocation && fileExists )
	{
	    warnings.add( createSpecifiedSourceWarning( customFile, sysprops ) );
	    return new WarnedConfig( ConfigFactory.load(), warnings );
	}
	else if ( !fileExists ) 
	    return new WarnedConfig( ConfigFactory.load(), warnings );
	else 
	{
	    Config out = defaultOverrides().withFallback( parseFile( customFile ).withFallback( defaultApplication().withFallback( defaultReference() ) ) );
	    return new WarnedConfig( out, warnings );
	}
    }

    private static String createSpecifiedSourceWarning( File customFile, Properties sysprops )
    {
	boolean first = true;;
	StringBuilder sb = new StringBuilder();
	sb.append( "Config file " );
	sb.append( customFile.getAbsolutePath() );
	sb.append( " will be ignored because a location has been explicitly set via System.properties. [");
	if ( sysprops.containsKey( "config.resource" ) )
	{
	    sb.append( "config.resource=" + sysprops.getProperty( "config.resource" ) );
	    first = false;
	}
	if ( sysprops.containsKey( "config.file" ) )
	{
	    if(! first) sb.append( ", " );
	    sb.append( "config.file=" + sysprops.getProperty( "config.file" ) );
	    first = false;
	}
	if ( sysprops.containsKey( "config.url" ) )
	{
	    if(! first) sb.append( ", " );
	    sb.append( "config.url=" + sysprops.getProperty( "config.url" ) );
	    first = false;
	}
	sb.append("]");
	return sb.toString();
    }

    private HoconUtils()
    {}
}
