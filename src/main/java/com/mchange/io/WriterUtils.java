package com.mchange.io;

import java.io.Writer;
import java.io.IOException;
import com.mchange.util.RobustMessageLogger;

/**
 * @deprecated use com.mchange.v1.io.WriterUtils
 */
public final class WriterUtils
{
  public static void attemptClose(Writer w)
    {attemptClose(w, null);}

  public static void attemptClose(Writer w, RobustMessageLogger logger)
    {
      try
	{w.close();}
      catch (IOException e)
	{if (logger != null) logger.log(e, "IOException trying to close Writer");}
      catch (NullPointerException e)
	{if (logger != null) logger.log(e, "NullPointerException trying to close Writer");}
    }

  private WriterUtils()
    {}
}
