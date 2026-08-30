package com.mchange.v3.filecache;

import java.net.URL;

public interface FileCacheKey
{
    public URL getURL(); //Any potential Exceptions should be thrown by the constructor
    public String getCacheFilePath();

    public boolean equals( Object o );
    public int hashCode();
}
