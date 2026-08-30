package com.mchange.io;

import java.io.*;

public interface IOStringObjectMap
{
  public Object  get         (String key)               throws IOException;
  public void    put         (String key, Object value) throws IOException;
  public boolean putNoReplace(String key, Object value) throws IOException;
  public boolean remove      (String key)               throws IOException;
  public boolean containsKey (String key)               throws IOException;
  public IOStringEnumeration keys()                     throws IOException;
}
