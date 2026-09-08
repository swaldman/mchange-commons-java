package com.mchange.util;

import java.io.IOException;
import java.util.Iterator;

public interface FailSuppressedMessageLogger extends RobustMessageLogger
{
  /**
   *  returns an Iterator of IOExceptions,
   *  or null if there have been no failures
   */
  public Iterator<IOException> getFailures();

  public void clearFailures();
}
