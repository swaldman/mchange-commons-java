package com.mchange.util;

public interface RobustMessageLogger extends MessageLogger
{
  public void log(String message);
  public void log(Throwable t, String message);
}
