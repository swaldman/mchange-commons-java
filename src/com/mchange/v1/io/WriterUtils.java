package com.mchange.v1.io;

import java.io.Writer;
import java.io.IOException;

public final class WriterUtils
{
    public static void attemptClose(Writer os)
    {
      try
	{if (os != null) os.close();}
      catch (IOException e)
	  {e.printStackTrace();}
    }

  private WriterUtils()
    {}
}
