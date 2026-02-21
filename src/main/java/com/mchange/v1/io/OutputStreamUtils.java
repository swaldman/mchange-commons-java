package com.mchange.v1.io;

import com.mchange.v2.log.*;
import java.io.OutputStream;
import java.io.IOException;

public final class OutputStreamUtils
{
    private final static MLogger logger = MLog.getLogger( OutputStreamUtils.class );

    public static void attemptClose(OutputStream os, MLogger mlogger)
    {
      try
	  {if (os != null) os.close();}
      catch (IOException e)
	  {
		if ( mlogger.isLoggable( MLevel.WARNING ) )
		    mlogger.log( MLevel.WARNING, "OutputStream close FAILED.", e );
	      //e.printStackTrace();
	  }
    }

    public static void attemptClose(OutputStream os)
    { attemptClose( os, logger ); }
    
    public static void attemptCloseIgnore(OutputStream os)
    { attemptClose( os, NullMLogger.instance() ); }
    
    private OutputStreamUtils()
    {}
}
