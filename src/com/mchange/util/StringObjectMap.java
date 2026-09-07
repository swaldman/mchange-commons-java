package com.mchange.util;

import com.mchange.io.*;

public interface StringObjectMap extends IOStringObjectMap
{
  @Override
  public Object  get         (String key);
  @Override
  public void    put         (String key, Object value);
  @Override
  public boolean putNoReplace(String key, Object value);
  @Override
  public boolean remove      (String key);
  @Override
  public boolean containsKey (String key);
  @Override
  public IOStringEnumeration keys();
  public StringEnumeration   mkeys();
}
