package com.mchange.util.impl;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import com.mchange.util.ObjectCache;

public abstract class SoftReferenceObjectCache implements ObjectCache
{
  Map store = new HashMap();
  
  public synchronized Object find(Object key) throws Exception
    {
      Reference ref = (Reference) store.get(key);
      Object out;
      if (ref == null || (out = ref.get()) == null || isDirty(key, out))
	{
	  out = createFromKey(key);
	  store.put(key, new SoftReference(out));
	}
      return out;
    }

  protected boolean isDirty(Object key, Object cached)
    {return false;}

  protected abstract Object createFromKey(Object key) throws Exception;
}
