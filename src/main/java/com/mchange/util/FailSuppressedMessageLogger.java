package com.mchange.util;

import java.util.Iterator;

public interface FailSuppressedMessageLogger extends RobustMessageLogger
{
  /**
   *  returns an Iterator of IOExceptions,
   *  or null if there have been no failures
   */
  public Iterator getFailures();

  public void clearFailures();
}
