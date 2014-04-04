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

package com.mchange.v2.io;

import java.io.*;

public final class FileUtils
{
    public static File findRelativeToParent(File parentDir, File file) throws IOException
    {
	String parentPath = parentDir.getPath();
	String filePath = file.getPath();
	if (! filePath.startsWith( parentPath ) )
	    throw new IllegalArgumentException( filePath + " is not a child of " + parentPath + " [no transformations or canonicalizations tried]" );
	String maybeRelative = filePath.substring( parentPath.length() );
	File out = new File( maybeRelative );
	if ( out.isAbsolute() )
	    out = new File( out.getPath().substring(1) );
	return out;
    }

    public static long diskSpaceUsed( File maybeDir ) throws IOException
    {
	long sum = 0;
	for (FileIterator ff = DirectoryDescentUtils.depthFirstEagerDescent( maybeDir ); ff.hasNext();)
	    {
		File addMe = ff.nextFile();
		//System.err.println("diskSpacedUsed() -- checking: " + addMe);
		if (! addMe.isFile())
		    continue;

		sum += addMe.length();
	    }
	return sum;
    }

    public static void touchExisting( File file ) throws IOException
    {
	if ( file.exists() )
	    unguardedTouch( file );
    }

    public static void touch( File file ) throws IOException
    {
	if (! file.exists() )
	    createEmpty( file );
	unguardedTouch( file );
    }

    public static void createEmpty( File file ) throws IOException
    {
	RandomAccessFile raf = null;
	try
	    {
		raf = new RandomAccessFile( file, "rws" );
		raf.setLength( 0 );
	    }
	finally
	    {
		try { if (raf != null) raf.close(); }
		catch ( IOException e )
		    { e.printStackTrace(); }
	    }
    }

    private static void unguardedTouch( File file ) throws IOException
    { file.setLastModified( System.currentTimeMillis() ); }	

    private FileUtils()
    {}
}
