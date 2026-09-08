package com.mchange.util.impl;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/** @deprecated implements the deprecated com.mchange.util.ObjectCache */
@Deprecated
public abstract class SoftReferenceObjectCache implements com.mchange.util.ObjectCache
{
  Map<Object,Reference<Object>> store = new HashMap<Object,Reference<Object>>();
  
  @Override
  public synchronized Object find(Object key) throws Exception
    {
      Reference<Object> ref = store.get(key);
      Object out;
      if (ref == null || (out = ref.get()) == null || isDirty(key, out))
	{
	  out = createFromKey(key);
	  store.put(key, new SoftReference<Object>(out));
	}
      return out;
    }

  protected boolean isDirty(Object key, Object cached)
    {return false;}

  protected abstract Object createFromKey(Object key) throws Exception;
}
