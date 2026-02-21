package com.mchange.v3.filecache;

import java.io.FileNotFoundException;

public class FileNotCachedException extends FileNotFoundException
{
    FileNotCachedException( String message )
    { super( message ); }

    FileNotCachedException()
    { super(); }
}

