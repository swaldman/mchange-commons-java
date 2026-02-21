package com.mchange.util.impl;

import java.util.Hashtable;
import com.mchange.util.ObjectCache;

public abstract class NoGCObjectCache implements ObjectCache
{
  Hashtable store = new Hashtable();
  
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
