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

package com.mchange.v3.filecache;

import java.io.*;
import java.net.*;
import java.util.*;

import com.mchange.v2.io.*;
import com.mchange.v2.log.*;

import com.mchange.v1.io.InputStreamUtils;
import com.mchange.v1.io.OutputStreamUtils;

public final class FileCache
{
    final static MLogger logger = MLog.getLogger( FileCache.class );

    final File    cacheDir;
    final int     buffer_size;
    final boolean read_only;

    final List<URLFetcher> fetchers;

    private InputStream fetchURL(URL u) throws IOException
    {
	List<IOException> exceptions = null;
	for (URLFetcher fetcher : fetchers)
	    {
		try { return fetcher.openStream( u, logger ); }
		catch (FileNotFoundException e)
		    { throw e; }
		catch (IOException e)
		    {
			if (logger.isLoggable( MLevel.FINE ))
			    logger.log( MLevel.FINE, "URLFetcher " + fetcher + " failed on Exception. Will try next fetcher, if any.", e);
			if (exceptions == null)
			    exceptions = new LinkedList<IOException>();
			exceptions.add(e);
		    }
	    }
	if (logger.isLoggable( MLevel.WARNING ))
	    {
		logger.log( MLevel.WARNING, "All URLFetchers failed on URL " + u);
		for (int i = 0, len = exceptions.size(); i < len; ++i)
		    logger.log(MLevel.WARNING, "URLFetcher Exception #" + (i+1), exceptions.get(i));
	    }

	// hopefully we returned long before we reached here...
	throw new IOException("Failed to fetch URL '" + u + "'.");
    } 


    public FileCache(File cacheDir, int buffer_size, boolean read_only) throws IOException
    { this( cacheDir, buffer_size, read_only, Collections.singletonList( (URLFetcher) URLFetchers.DEFAULT ) ); }

    public FileCache(File cacheDir, int buffer_size, boolean read_only, URLFetcher... fetchers) throws IOException
    { this( cacheDir, buffer_size, read_only, Arrays.asList( fetchers ) ); }

    public FileCache(File cacheDir, int buffer_size, boolean read_only, List<URLFetcher> fetchers) throws IOException
    {
	this.cacheDir    = cacheDir;
	this.buffer_size = buffer_size;
	this.read_only   = read_only;

	this.fetchers = Collections.unmodifiableList( fetchers );

	if (cacheDir.exists())
	    {
		if (! cacheDir.isDirectory())
		    loggedIOException(MLevel.SEVERE, cacheDir + "exists and is not a directory. Can't use as cacheDir.");
		else if (! cacheDir.canRead() )
		    loggedIOException(MLevel.SEVERE, cacheDir + "must be readable.");
		else if (! cacheDir.canWrite() && ! read_only)
		    loggedIOException(MLevel.SEVERE, cacheDir + "not writable, and not read only.");
	    }
	else if (! cacheDir.mkdir())
	    loggedIOException(MLevel.SEVERE, cacheDir + "does not exist and could not be created.");
	
	//okay... we have a cacheDir

    }

    public void ensureCached(FileCacheKey key, boolean force_reacquire) throws IOException
    {
	File f = file( key );

	if (! read_only )
	    {
		if ( force_reacquire || (!f.exists()) )
		    {
			InputStream  is = null;
			OutputStream os = null;
			
			try
			    {
				if ( logger.isLoggable( MLevel.FINE ) )
				    logger.log( MLevel.FINE, "Caching file for " + key + " to " + f.getAbsolutePath() + "...");
				File dir = f.getParentFile();
				if (! dir.exists()) dir.mkdirs();

				//is = new BufferedInputStream( key.getURL().openStream(), buffer_size );
				is = new BufferedInputStream( fetchURL( key.getURL() ), buffer_size );
				os = new BufferedOutputStream( new FileOutputStream( f ), buffer_size );
				for (int b = is.read(); b >= 0; b = is.read())
				    os.write(b);

				if ( logger.isLoggable( MLevel.INFO ) )
				    logger.log( MLevel.INFO, "Cached file for " + key + ".");
			    }
			catch (IOException e)
			    {
				logger.log( MLevel.WARNING, "An exception occurred while caching file for " + key + ". Deleting questionable cached file.", e);
				f.delete();
				throw e;
			    }
			finally
			    {
				InputStreamUtils.attemptClose( is );
				OutputStreamUtils.attemptClose( os );
			    }
		    }
		else
		    {
			if ( logger.isLoggable( MLevel.FINE ) )
			    logger.log( MLevel.FINE, "File for " + key + " already exists and force_reacquire is not set.");
		    }
	    }
	else if ( force_reacquire )
	    {
		String message = "force_reacquire canot be set on a read_only FileCache.";
		IllegalArgumentException e = new IllegalArgumentException( message );
		logger.log( MLevel.WARNING, message, e );
		throw e;
	    }
	else if (! f.exists() )
	    {
		String message = "Cache is read only, and file for key '" + key + "' does not exist.";
		FileNotCachedException e = new FileNotCachedException( message );
		logger.log( MLevel.FINE, message, e );
		throw e;
	    }
    }

    public InputStream fetch(FileCacheKey key, boolean force_reacquire) throws IOException
    {
	ensureCached( key, force_reacquire);
	return new FileInputStream( file(key) );
    }

    public boolean isCached(FileCacheKey key) throws IOException
    { return file( key ).exists(); }

    final static FileFilter NOT_DIR_FF = new FileFilter()
    {
	public boolean accept(File f)
	{ return ! f.isDirectory(); }
	};

    static class NotDirAndFileFilter implements FileFilter
    {
	FileFilter ff;

	NotDirAndFileFilter(FileFilter ff)
	{ this.ff = ff; }

	public boolean accept(File f)
	{ return (! f.isDirectory()) && ff.accept(f); }
    }


    public int countCached() throws IOException
    {
	int count = 0;
	for( FileIterator fi = DirectoryDescentUtils.depthFirstEagerDescent( cacheDir, NOT_DIR_FF, false ); fi.hasNext(); )
	    {
		fi.next();
		++count;
	    }
	return count;
    }

    public int countCached(FileFilter filter) throws IOException
    {
	int count = 0;
	for( FileIterator fi = DirectoryDescentUtils.depthFirstEagerDescent( cacheDir, new NotDirAndFileFilter( filter ), false ); fi.hasNext(); )
	    {
		fi.next();
		++count;
	    }
	return count;
    }

    public File fileForKey(FileCacheKey key)
    { return file(key); }

    private File file(FileCacheKey key)
    { return new File( cacheDir, key.getCacheFilePath() ); }

    private void loggedIOException(MLevel lvl, String msg) throws IOException
    {
	IOException e = new IOException( msg );
	logger.log(lvl, msg, e);
	throw e;
    }
}
