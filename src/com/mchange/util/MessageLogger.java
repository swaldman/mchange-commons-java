package com.mchange.util;

import java.io.IOException;

public interface MessageLogger
{
  public void log(String message) throws IOException;
  public void log(Throwable t, String message) throws IOException;
}
