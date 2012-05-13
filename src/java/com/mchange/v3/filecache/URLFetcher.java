package com.mchange.v3.filecache;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

import com.mchange.v2.log.*;

public interface URLFetcher
{
    public InputStream openStream( URL u, MLogger logger ) throws IOException;
}