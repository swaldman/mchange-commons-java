package com.mchange.v3.filecache;

import java.net.URL;

public interface FileCacheKey
{
    public URL getURL(); //Any potential Exceptions should be thrown by the constructor

    /**
     *  The path, relative to a FileCache's cache directory, at which this key's URL will
     *  be cached. It is resolved as new File(cacheDir, path), which normalizes nothing, so
     *  an implementation must ensure the path stays beneath that directory: no leading
     *  '/', no ".." segment, and on Windows no backslash. See {@link RelativePathFileCacheKey}.
     */
    public String getCacheFilePath();

    @Override
    public boolean equals( Object o );
    @Override
    public int hashCode();
}
