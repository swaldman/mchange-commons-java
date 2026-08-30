package com.mchange.util;

/** @deprecated use com.mchange.v1.util.ObjectCache */
public interface ObjectCache
{
  public Object find(Object key) throws Exception;
}
