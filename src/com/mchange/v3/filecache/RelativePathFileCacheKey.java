package com.mchange.v3.filecache;

import java.net.URL;
import java.net.MalformedURLException;

public class RelativePathFileCacheKey implements FileCacheKey
{
    final URL url;
    final String relPath;

    public RelativePathFileCacheKey(URL parentURL, String relPath) throws MalformedURLException, IllegalArgumentException
    {
	if ( parentURL == null || relPath == null )
	    throw new IllegalArgumentException("parentURL [" + parentURL + "] and relative path [" + relPath + "] must be non-null");
        if (!parentURL.toString().endsWith("/"))
            throw new IllegalArgumentException("Parent URL '" + parentURL + "' must refer to a logical directory, and so end with a '/' character.");

	String trimmed = relPath.trim();

	if ( trimmed.length() == 0)
	    throw new IllegalArgumentException("relative path [" + relPath + "] must not be a blank string");
	else if ( !trimmed.equals( relPath ) )
	    throw new IllegalArgumentException("relative path [" + relPath + "] must not begin or end with whitespace.");
	if ( relPath.startsWith("/") )
	    throw new IllegalArgumentException("Path must be relative, '" + relPath + "' begins with '/'.");
        for ( String segment : relPath.split("/") )
            if ( "..".equals( segment ) )
                throw new IllegalArgumentException("Relative path '" + relPath + "' must not contain a '..' segment: getCacheFilePath() is resolved against the cache directory, so '..' escapes it.");

	this.url = new URL( parentURL, relPath );

        // new URL(context,spec) lets a spec bearing its own scheme replace the context
        // outright, which is not a path beneath parentURL at all.
        if ( ! this.url.toString().startsWith( parentURL.toString() ) )
            throw new IllegalArgumentException("Relative path '" + relPath + "' does not resolve beneath parent URL '" + parentURL + "'.");
        if ( relPath.indexOf('\\') >= 0 )
            throw new IllegalArgumentException("Relative path '" + relPath + "' must not contain a backslash: it is a URL path, and on Windows a backslash is a file separator that could escape the cache directory.");

	this.relPath = relPath;
    }

    @Override
    public URL getURL()
    { return url; }

    @Override
    public String getCacheFilePath()
    { return relPath; }

    @Override
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

    @Override
    public int hashCode()
    { return url.hashCode() ^ relPath.hashCode(); }
}

