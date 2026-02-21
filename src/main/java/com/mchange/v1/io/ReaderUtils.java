package com.mchange.v1.io;

import java.io.Reader;
import java.io.IOException;

public final class ReaderUtils
{
    public static void attemptClose(Reader r)
    {
      try
	{if (r != null) r.close();}
      catch (IOException e)
	  {e.printStackTrace();}
    }

    private ReaderUtils()
    {}
}
