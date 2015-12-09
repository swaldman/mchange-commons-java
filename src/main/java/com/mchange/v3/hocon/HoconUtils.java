/*
 * Distributed as part of mchange-commons-java 0.2.11
 *
 * Copyright (C) 2015 Machinery For Change, Inc.
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
import com.mchange.v2.cfg.*;
import com.typesafe.config.*;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

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
    private HoconUtils()
    {}
}
