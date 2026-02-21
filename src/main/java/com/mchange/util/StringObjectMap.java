package com.mchange.util;

import com.mchange.io.*;

public interface StringObjectMap extends IOStringObjectMap
{
  public Object  get         (String key);
  public void    put         (String key, Object value);
  public boolean putNoReplace(String key, Object value);
  public boolean remove      (String key);
  public boolean containsKey (String key);
  public IOStringEnumeration keys();
  public StringEnumeration   mkeys();
}
