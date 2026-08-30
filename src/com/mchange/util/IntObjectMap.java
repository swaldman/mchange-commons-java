package com.mchange.util;

public interface IntObjectMap
{
  public Object         get(int num);
  public void           put(int num, Object value);
  public boolean        putNoReplace(int num, Object value);
  public Object         remove(int num);
  public boolean        containsInt(int num);
  public int            getSize();
  public void           clear();
  public IntEnumeration ints();
}
