package com.mchange.lang;

import java.io.*;

public final class ThrowableUtils
{
  public static String extractStackTrace(Throwable t)
    {
      StringWriter me = new StringWriter();
      PrintWriter pw = new PrintWriter(me);
      t.printStackTrace(pw);
      pw.flush();
      return me.toString();
    }

    public static boolean isChecked(Throwable t)
    { 
	return 
	    t instanceof Exception &&
	    ! (t instanceof RuntimeException);
    }

    public static boolean isUnchecked(Throwable t)
    { return ! isChecked( t ); }
      
    private ThrowableUtils()
    {}
}
