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

package com.mchange.v2.cfg;

import java.util.*;
import java.io.*;

import static com.mchange.v2.cfg.DelayedLogItem.*;

// external clients should go through the MConfig facade.
final class ConfigUtils
{
    private final static String[] DFLT_VM_RSRC_PATHFILES    = new String[] {"/com/mchange/v2/cfg/vmConfigResourcePaths.txt", "/mchange-config-resource-paths.txt"};
    private final static String[] HARDCODED_DFLT_RSRC_PATHS = new String[] 
	{
	    "/mchange-commons.properties", 
	    "hocon:/reference,/application,/", 
	    "/"
	};

    final static String[] NO_PATHS                  = new String[0];

    //MT: protected by class' lock
    static MultiPropertiesConfig vmConfig = null;

    //public static MultiPropertiesConfig read(String[] resourcePath, MLogger logger)
    //{ return new BasicMultiPropertiesConfig( resourcePath, logger ); }

    static MultiPropertiesConfig read(String[] resourcePath, List delayedLogItems)
    { return new BasicMultiPropertiesConfig( resourcePath, delayedLogItems ); }

    public static MultiPropertiesConfig read(String[] resourcePath)
    { return new BasicMultiPropertiesConfig( resourcePath ); }

    /**
     *  Later entries in the configs array override earlier entries.
     */
    public static MultiPropertiesConfig combine( MultiPropertiesConfig[] configs )
    { return new CombinedMultiPropertiesConfig( configs ).toBasic(); }

    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources )
    { return readVmConfig( defaultResources, preemptingResources, (List) null ); }

    /*
    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources, MLogger logger)
    {
	List items = new ArrayList();
	MultiPropertiesConfig out = readVmConfig( defaultResources, preemptingResources, items );
	items.addAll( out.getDelayedLogItems() );
	for (Iterator ii = items.iterator(); ii.hasNext(); )
	{
	    DelayedLogItem item = (DelayedLogItem) ii.next();
	    logger.log( item.getLevel(), item.getText(), item.getException() );
	}
	return out;
    }
    */

    static List vmCondensedPaths(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    { return condensePaths( new String[][]{ defaultResources, vmResourcePaths( delayedLogItemsOut ), preemptingResources } ); }

    static String stringFromPathsList( List pathsList )
    {
	StringBuffer sb = new StringBuffer(2048);
	for ( int i = 0, len = pathsList.size(); i < len; ++i)
	    {
		if ( i != 0 ) sb.append(", ");
		sb.append( pathsList.get(i) );
	    }
	return sb.toString();
    }

    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    {
	defaultResources = ( defaultResources == null ? NO_PATHS : defaultResources );
	preemptingResources = ( preemptingResources == null ? NO_PATHS : preemptingResources );
	List pathsList = vmCondensedPaths( defaultResources, preemptingResources, delayedLogItemsOut );
	
	if ( delayedLogItemsOut != null )
	    delayedLogItemsOut.add( new DelayedLogItem(Level.FINER, "Reading VM config for path list " + stringFromPathsList( pathsList ) ) );

	return read( (String[]) pathsList.toArray(new String[pathsList.size()]), delayedLogItemsOut );
    }

    private static List condensePaths(String[][] pathLists)
    {
	// we do this in reverse, so that the "first" time
	// we encounter a path becomes the last in the resultant
	// list. that is, we want redundantly specified paths 
	// to have their maximum specified preference

	Set pathSet = new HashSet();
	List reverseMe = new ArrayList();
	for ( int i = pathLists.length; --i >= 0; )
	    for( int j = pathLists[i].length; --j >= 0; )
	    {
		String path = pathLists[i][j];
		if (! pathSet.contains( path ) )
		{
		    pathSet.add( path );
		    reverseMe.add( path );
		}
	    }
	 Collections.reverse( reverseMe );
	 return reverseMe;
    }

    private static List readResourcePathsFromResourcePathsTextFile( String resourcePathsTextFileResourcePath,  List delayedLogItemsOut )
    {
	List rps = new ArrayList();

	BufferedReader br = null;
	try
	    {
		InputStream is = MultiPropertiesConfig.class.getResourceAsStream( resourcePathsTextFileResourcePath );
		if ( is != null )
		    {
			br = new BufferedReader( new InputStreamReader( is, "8859_1" ) );
			String rp;
			while ((rp = br.readLine()) != null)
			    {
				rp = rp.trim();
				if ("".equals( rp ) || rp.startsWith("#"))
				    continue;
				
				rps.add( rp );
			    }

			if ( delayedLogItemsOut != null )
			    delayedLogItemsOut.add( new DelayedLogItem( Level.FINEST, String.format( "Added paths from resource path text file at '%s'", resourcePathsTextFileResourcePath ) ) );
		    }
		else if ( delayedLogItemsOut != null )
		    delayedLogItemsOut.add( new DelayedLogItem( Level.FINEST, String.format( "Could not find resource path text file for path '%s'. Skipping.", resourcePathsTextFileResourcePath ) ) );

	    }
	catch (IOException e)
	    { e.printStackTrace(); }
	finally
	    {
		try { if ( br != null ) br.close(); }
		catch (IOException e) { e.printStackTrace(); }
	    }

	return rps;
    }

    private static List readResourcePathsFromResourcePathsTextFiles( String[] resourcePathsTextFileResourcePaths, List delayedLogItemsOut )
    {
	List out = new ArrayList();
	for ( int i = 0, len = resourcePathsTextFileResourcePaths.length; i < len; ++i )
	    out.addAll( readResourcePathsFromResourcePathsTextFile(  resourcePathsTextFileResourcePaths[i], delayedLogItemsOut ) );
	return out;
    }

    private static String[] vmResourcePaths( List delayedLogItemsOut ) 
    {
	List paths = vmResourcePathList(  delayedLogItemsOut );
	return (String[]) paths.toArray( new String[ paths.size() ] );
    }

    private static List vmResourcePathList( List delayedLogItemsOut )
    {
	List pathsFromFiles = readResourcePathsFromResourcePathsTextFiles( DFLT_VM_RSRC_PATHFILES, delayedLogItemsOut );
	List rps;
	if ( pathsFromFiles.size() > 0 )
	    rps = pathsFromFiles;
	else
	    rps = Arrays.asList( HARDCODED_DFLT_RSRC_PATHS );
	return rps;
    }
    
    public synchronized static MultiPropertiesConfig readVmConfig()
    { return readVmConfig( (List) null ); }

    /*
    public synchronized static MultiPropertiesConfig readVmConfig( MLogger logger )
    {
	List items = new ArrayList();
	MultiPropertiesConfig out = readVmConfig( items );
	items.addAll( out.getDelayedLogItems() );
	for (Iterator ii = items.iterator(); ii.hasNext(); )
	{
	    DelayedLogItem item = (DelayedLogItem) ii.next();
	    logger.log( item.getLevel(), item.getText(), item.getException() );
	}
	return out;
    }
    */

    public synchronized static MultiPropertiesConfig readVmConfig( List delayedLogItemsOut )
    {
	if ( vmConfig == null )
	    {
		List rps = vmResourcePathList( delayedLogItemsOut );
		vmConfig = new BasicMultiPropertiesConfig( (String[]) rps.toArray( new String[ rps.size() ] ) ); 
	    }
	return vmConfig;
    }

    public static synchronized boolean foundVmConfig()
    { return vmConfig != null; }

    public static void dumpByPrefix( MultiPropertiesConfig mpc, String pfx )
    {
	Properties props = mpc.getPropertiesByPrefix(pfx);
	Map m = new TreeMap();
	m.putAll( props );
	for ( Iterator ii = m.entrySet().iterator(); ii.hasNext(); )
	{
	    Map.Entry entry = (Map.Entry) ii.next();
	    System.err.println( entry.getKey() + " --> " + entry.getValue() );
	}
    }

    private ConfigUtils()
    {}
}
