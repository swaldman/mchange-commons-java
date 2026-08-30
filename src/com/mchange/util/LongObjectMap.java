package com.mchange.util;

public interface LongObjectMap
{
  public Object  get(long num);
  public void    put(long num, Object value);
  public boolean putNoReplace(long num, Object value);
  public Object  remove(long num);
  public boolean containsLong(long num);
  public long    getSize();
}
