/*
 * Distributed as part of mchange-commons-java v.0.2.3.3
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v2.io;

import java.io.*;

public final class FileUtils
{
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
