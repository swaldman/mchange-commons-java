package com.mchange.io;

import java.io.Reader;
import java.io.IOException;
import com.mchange.util.RobustMessageLogger;

public final class ReaderUtils
{
  public static void attemptClose(Reader r)
    {attemptClose(r, null);}

  public static void attemptClose(Reader r, RobustMessageLogger logger)
    {
      try
	{r.close();}
      catch (IOException e)
	{if (logger != null) logger.log(e, "IOException trying to close Reader");}
      catch (NullPointerException e)
	{if (logger != null) logger.log(e, "NullPointerException trying to close Reader");}
    }

  private ReaderUtils()
    {}
}
