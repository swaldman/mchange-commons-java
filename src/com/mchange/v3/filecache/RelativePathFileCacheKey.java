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

