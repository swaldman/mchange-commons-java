package com.mchange.net;

import java.io.IOException;
import java.net.Socket;
import com.mchange.util.RobustMessageLogger;

public final class SocketUtils
{
  public static void attemptClose(Socket s)
    {attemptClose(s, null);}

  public static void attemptClose(Socket s, RobustMessageLogger logger)
    {
      try
	{s.close();}
      catch (IOException e)
	{if (logger != null) logger.log(e, "IOException trying to close Socket");}
      catch (NullPointerException e)
	{if (logger != null) logger.log(e, "NullPointerException trying to close Socket");}
    }

  private SocketUtils()
    {}
}


