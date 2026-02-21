package com.mchange.util.impl;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.io.IOException;
import com.mchange.util.MessageLogger;
import com.mchange.util.FailSuppressedMessageLogger;

public class FSMessageLoggerAdapter implements FailSuppressedMessageLogger
{
  MessageLogger inner;
  List          failures = null;

  public FSMessageLoggerAdapter(MessageLogger wrapMe)
    {this.inner = wrapMe;}

  /* we presume accesses to inner are already thread-safe */
  /* and do not synchronize.                              */
  public void log(String message)
    {
      try {inner.log(message);}
      catch (IOException e)
	{addFailure(e);}
    }

  public void log(Throwable t, String message)
    {
      try {inner.log(t, message);}
      catch (IOException e)
	{addFailure(e);}
    }

  public synchronized Iterator getFailures()
    {
      if (inner instanceof FailSuppressedMessageLogger)
	return ((FailSuppressedMessageLogger) inner).getFailures();
      else return (failures != null ? failures.iterator() : null);
    }

  public synchronized void clearFailures()
    {
      if (inner instanceof FailSuppressedMessageLogger)
	((FailSuppressedMessageLogger) inner).clearFailures();
      else failures = null;
    }
  
  private synchronized void addFailure(IOException e)
    {
      if (failures == null)
	failures = new LinkedList();
      failures.add(e);
    }
}
