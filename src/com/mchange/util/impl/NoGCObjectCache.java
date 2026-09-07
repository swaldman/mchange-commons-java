package com.mchange.util.impl;

import java.util.Hashtable;

/** @deprecated implements the deprecated com.mchange.util.ObjectCache */
@Deprecated
public abstract class NoGCObjectCache implements com.mchange.util.ObjectCache
{
  Hashtable store = new Hashtable();
  
  @Override
  public Object find(Object key) throws Exception
    {
      Object out = store.get(key);
      if (out == null || isDirty(key, out))
	{
	  out = createFromKey(key);
	  store.put(key, out);
	}
      return out;
    }

  protected boolean isDirty(Object key, Object cached)
    {return false;}

  protected abstract Object createFromKey(Object key) throws Exception;
}
