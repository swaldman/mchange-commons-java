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
import java.util.*;

public final class DirectoryDescentUtils
{
    /**
     * @return FileIterator over all files and dierctories beneath root
     */
    public static FileIterator depthFirstEagerDescent(File root) 
	throws IOException
    { return depthFirstEagerDescent( root, null, false ); }

    /**
     * @return FileIterator over all files and directories beneath root that
     *         match filter.
     *
     * @param  canonical file paths will be canonicalized if true
     */
    public static FileIterator depthFirstEagerDescent(File root, 
						      FileFilter filter, 
						      boolean canonical) 
	throws IOException
    { 
	List list = new LinkedList();
	Set  seenDirex = new HashSet();
	depthFirstEagerDescend(root, filter, canonical, list, seenDirex);
	return new IteratorFileIterator( list.iterator() );
    }

    public static void addSubtree( File root, FileFilter filter, boolean canonical, Collection addToMe ) throws IOException
    {
	Set  seenDirex = new HashSet();
	depthFirstEagerDescend(root, filter, canonical, addToMe, seenDirex);
    }

    private static void depthFirstEagerDescend(File dir, FileFilter filter, boolean canonical, 
					       Collection addToMe, Set seenDirex)
	throws IOException
    {
	String canonicalPath = dir.getCanonicalPath();
	if (! seenDirex.contains( canonicalPath ) )
	    {
		if ( filter == null || filter.accept( dir ) )
		    addToMe.add( canonical ? new File( canonicalPath ) : dir );
		seenDirex.add( canonicalPath );
		String[] babies = dir.list();
		for (int i = 0, len = babies.length; i < len; ++i)
		    {
			File baby = new File(dir, babies[i]);
			if (baby.isDirectory())
			    depthFirstEagerDescend(baby, filter, canonical, addToMe, seenDirex);
			else
			    if ( filter == null || filter.accept( baby ) )
				addToMe.add( canonical ? baby.getCanonicalFile() : baby );
		    }
	    }
    }

    private static class IteratorFileIterator implements FileIterator
    {
	Iterator ii;
	Object last;

	IteratorFileIterator(Iterator ii)
	{ this.ii = ii; }

	public File nextFile() throws IOException
	{ return (File) next(); }

	public boolean hasNext() throws IOException
	{ return ii.hasNext(); }

	public Object next() throws IOException
	{ return (last = ii.next()); }

	public void remove() throws IOException
	{
	    if (last != null)
		{
		    ((File) last).delete();
		    last = null;
		}
	    else
		throw new IllegalStateException();
	}

	public void close() throws IOException
	{}
    }

    private DirectoryDescentUtils()
    {}

    public static void main(String[] argv)
    {
	try
	    {
		FileIterator fii = depthFirstEagerDescent( new File(argv[0]) );
		while (fii.hasNext())
		    System.err.println( fii.nextFile().getPath() );
	    }
	catch (Exception e)
	    { e.printStackTrace(); }
    }
}
