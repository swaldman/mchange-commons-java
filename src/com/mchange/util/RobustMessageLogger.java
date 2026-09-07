package com.mchange.util;

public interface RobustMessageLogger extends MessageLogger
{
  @Override
  public void log(String message);
  @Override
  public void log(Throwable t, String message);
}
