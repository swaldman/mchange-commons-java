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

import java.net.URL;
import java.net.MalformedURLException;

public class RelativePathFileCacheKey implements FileCacheKey
{
    final URL url;
    final String relPath;

    public RelativePathFileCacheKey(URL parentURL, String relPath) throws MalformedURLException, IllegalArgumentException
    {
	String trimmed = relPath.trim();

	if ( parentURL == null || relPath == null )
	    throw new IllegalArgumentException("parentURL [" + parentURL + "] and relative path [" + relPath + "] must be non-null");
	else if ( trimmed.length() == 0)
	    throw new IllegalArgumentException("relative path [" + relPath + "] must not be a blank string");
	else if ( !trimmed.equals( relPath ) )
	    throw new IllegalArgumentException("relative path [" + relPath + "] must not begin or end with whitespace.");
	if ( relPath.startsWith("/") )
	    throw new IllegalArgumentException("Path must be relative, '" + relPath + "' begins with '/'.");

	this.url = new URL( parentURL, relPath );
	this.relPath = relPath;
    }
    
    public URL getURL()
    { return url; }

    public String getCacheFilePath()
    { return relPath; }

    public boolean equals( Object o )
    {
	if (o instanceof RelativePathFileCacheKey)
	    {
		RelativePathFileCacheKey oo = (RelativePathFileCacheKey) o;
		return this.url.equals( oo.url ) && this.relPath.equals( oo.relPath );
	    }
	else
	    return false;
    }

    public int hashCode()
    { return url.hashCode() ^ relPath.hashCode(); }
}

