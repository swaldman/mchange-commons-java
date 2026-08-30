package com.mchange.io;

import java.io.OutputStream;
import java.io.IOException;
import com.mchange.util.RobustMessageLogger;

/** @deprecated use com.mchange.v1.io.OutputStreamUtils */
public final class OutputStreamUtils
{
  public static void attemptClose(OutputStream os)
    {attemptClose(os, null);}

  public static void attemptClose(OutputStream os, RobustMessageLogger logger)
    {
      try
	{os.close();}
      catch (IOException e)
	{if (logger != null) logger.log(e, "IOException trying to close OutputStream");}
      catch (NullPointerException e)
	{if (logger != null) logger.log(e, "NullPointerException trying to close OutputStream");}
    }

  private OutputStreamUtils()
    {}
}
