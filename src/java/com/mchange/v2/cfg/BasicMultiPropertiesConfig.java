/*
 * Distributed as part of mchange-commons-java v.0.2.4
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

package com.mchange.v2.cfg;

import java.util.*;
import java.io.*;
import com.mchange.v2.log.*;

public final class BasicMultiPropertiesConfig extends MultiPropertiesConfig
{
    final static String HOCON_CFG_SRC_CNAME = "com.mchange.v3.hocon.HoconPropertiesConfigSource";

    static final class SystemPropertiesConfigSource implements PropertiesConfigSource
    {
	public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception
	{
	    if ( "/".equals( identifier ) )
		return new Parse( System.getProperties(), Collections.<DelayedLogItem>emptyList() );
	    else
		throw new Exception(  String.format("Unexpected identifier for System properties: '%s'", identifier) );
	}
    }

    static boolean isHoconPath( String identifier )
    { return (identifier.length() > 6 && identifier.substring(0,6).toLowerCase().equals("hocon:")); }

    private static PropertiesConfigSource configSource( String identifier ) throws Exception
    {
	if ( "/".equals(identifier) )
	    return new SystemPropertiesConfigSource();
	else if ( isHoconPath( identifier ) )
	    return (PropertiesConfigSource) Class.forName( HOCON_CFG_SRC_CNAME ).newInstance();
	else
	    return new BasicPropertiesConfigSource();
    }

    String[] rps;
    Map  propsByResourcePaths;
    Map  propsByPrefixes;

    List parseMessages;

    Properties propsByKey;

    public BasicMultiPropertiesConfig(String[] resourcePaths)
    { this( resourcePaths, null ); }

    public BasicMultiPropertiesConfig(String[] resourcePaths, MLogger logger)
    {
	Map  pbrp = new HashMap();
	List goodPaths = new ArrayList();

	List<DelayedLogItem> pms = new LinkedList<DelayedLogItem>();

	for( int i = 0, len = resourcePaths.length; i < len; ++i )
	    {
		String rp = resourcePaths[i];

		try
		{
		    PropertiesConfigSource cs = configSource( rp );
		    PropertiesConfigSource.Parse parse = cs.propertiesFromSource( rp );
		    pbrp.put( rp, parse.getProperties() );
		    goodPaths.add( rp );
		    pms.addAll( parse.getDelayedLogItems() );
		}
		catch ( FileNotFoundException fnfe )
		{ pms.add( new DelayedLogItem( MLevel.FINE, String.format("The configuration file for resource identifier '%s' could not be found.", rp), fnfe) ); }
		catch ( Exception e )
		    { pms.add( new DelayedLogItem( MLevel.WARNING, String.format("An Exception occurred while processing configuration for resource identifier '%s' could not be found.", rp), e) );	}
	    }
	
	this.rps = (String[]) goodPaths.toArray( new String[ goodPaths.size() ] );
	this.propsByResourcePaths = Collections.unmodifiableMap( pbrp );
	this.parseMessages = Collections.unmodifiableList( pms );

	if ( logger != null )
	    for ( DelayedLogItem pm : pms )
		logger.log( pm.getLevel(), pm.getText(), pm.getException() );

	finishInit();
    }

    BasicMultiPropertiesConfig(String[] rps, Map propsByResourcePaths, List parseMessages)
    {
	this.rps                  = rps;
	this.propsByResourcePaths = propsByResourcePaths;
	this.parseMessages        = parseMessages;

	finishInit();
    }

    /**
     *  rps, propsByResourcePaths, and parseMessages should be set before finishInit()
     */
    private void finishInit()
    {
	this.propsByPrefixes = Collections.unmodifiableMap( extractPrefixMapFromRsrcPathMap(rps, propsByResourcePaths) );
	this.propsByKey = extractPropsByKey(rps, propsByResourcePaths);
    }

    public List getDelayedLogItems()
    { return parseMessages; }



    private static String extractPrefix( String s )
    {
	int lastdot = s.lastIndexOf('.');
	if ( lastdot < 0 )
	    return null;
	else
	    return s.substring(0, lastdot);
    }

    private static Properties findProps(String rp, Map pbrp)
    {
	//System.err.println("findProps( " + rp + ", ... )");
	Properties p;
	
	// MOVED THIS LOGIC INTO CONSTRUCTOR ABOVE, TO TREAT SYSTEM PROPS UNIFORMLY
	// WITH THE REST, AND TO AVOID UNINTENTIONAL ATTEMPTS TO READ RESOURCE "/"
	// AS STREAM -- swaldman, 2006-01-19
	
// 	if ( "/".equals( rp ) )
// 	    {
// 		try { p = System.getProperties(); }
// 		catch ( SecurityException e )
// 		    {
// 			System.err.println(BasicMultiPropertiesConfig.class.getName() +
// 					   " Read of system Properties blocked -- ignoring any configuration via System properties, and using Empty Properties! " +
// 					   "(But any configuration via a resource properties files is still okay!)"); 
// 			p = new Properties(); 
// 		    }
// 	    }
// 	else
	p = (Properties) pbrp.get( rp );
	
// 	System.err.println( p );

	return p;
    }

    private static Properties extractPropsByKey( String[] resourcePaths, Map pbrp )
    {
	Properties out = new Properties();
	for (int i = 0, len = resourcePaths.length; i < len; ++i)
	    {
		String rp = resourcePaths[i];
		Properties p = findProps( rp, pbrp );
		if (p == null)
		    {
			System.err.println("Could not find loaded properties for resource path: " + rp);
			continue;
		    }
		for (Iterator ii = p.keySet().iterator(); ii.hasNext(); )
		    {
			Object kObj = ii.next();
			if (!(kObj instanceof String))
			    {
				// note that we can not use the MLog library here, because initialization
				// of that library depends on this function.
				System.err.println( BasicMultiPropertiesConfig.class.getName() + ": " +
						    "Properties object found at resource path " +
						    ("/".equals(rp) ? "[system properties]" : "'" + rp + "'") +
						    "' contains a key that is not a String: " +
						    kObj);
				System.err.println("Skipping...");
				continue;
			    }
			Object vObj = p.get( kObj );
			if (vObj != null && !(vObj instanceof String))
			    {
				// note that we can not use the MLog library here, because initialization
				// of that library depends on this function.
				System.err.println( BasicMultiPropertiesConfig.class.getName() + ": " +
						    "Properties object found at resource path " +
						    ("/".equals(rp) ? "[system properties]" : "'" + rp + "'") +
						    " contains a value that is not a String: " +
						    vObj);
				System.err.println("Skipping...");
				continue;
			    }

			String key = (String) kObj;
			String val = (String) vObj;
			out.put( key, val );
		    }
	    }
	return out;
    }

    private static Map extractPrefixMapFromRsrcPathMap(String[] resourcePaths, Map pbrp)
    {
	Map out = new HashMap();
	//for( Iterator ii = pbrp.values().iterator(); ii.hasNext(); )
	for (int i = 0, len = resourcePaths.length; i < len; ++i)
	    {
		String rp = resourcePaths[i];
		Properties p = findProps( rp, pbrp );
		if (p == null)
		    {
			System.err.println(BasicMultiPropertiesConfig.class.getName() + " -- Could not find loaded properties for resource path: " + rp);
			continue;
		    }
		for (Iterator jj = p.keySet().iterator(); jj.hasNext(); )
		    {
			Object kObj = jj.next();
			if (! (kObj instanceof String))
			    {
				// note that we can not use the MLog library here, because initialization
				// of that library depends on this function.
				System.err.println( BasicMultiPropertiesConfig.class.getName() + ": " +
						    "Properties object found at resource path " +
						    ("/".equals(rp) ? "[system properties]" : "'" + rp + "'") +
						    "' contains a key that is not a String: " +
						    kObj);
				System.err.println("Skipping...");
				continue;
			    }

			String key = (String) kObj;
			String prefix = extractPrefix( key );
			while (prefix != null)
			    {
				Properties byPfx = (Properties) out.get( prefix );
				if (byPfx == null)
				    {
					byPfx = new Properties();
					out.put( prefix, byPfx );
				    }
				byPfx.put( key, p.get( key ) );

				prefix=extractPrefix( prefix );
			    }
		    }
	    }
	return out;
    }

    public String[] getPropertiesResourcePaths()
    { return (String[]) rps.clone(); }

    public Properties getPropertiesByResourcePath(String path)
    { 
	Properties out = ((Properties) propsByResourcePaths.get( path )); 
	return (out == null ? new Properties() : out);
    }

    public Properties getPropertiesByPrefix(String pfx)
    {
	Properties out = ((Properties) propsByPrefixes.get( pfx ));
	return (out == null ? new Properties() : out);
    }

    public String getProperty( String key )
    { return propsByKey.getProperty( key ); }

//    public Properties getProperties()
//    { return (Properties) propsByKey.clone(); }

    //TODO: Make this much prettier
    public String dump()
    { return String.format("[ propertiesByResourcePaths -> %s, propertiesByPrefixes -> %s ]", propsByResourcePaths, propsByPrefixes); }

    public String toString()
    { return super.toString() + " " + this.dump(); }
}
