package com.mchange.util.impl;

import com.mchange.util.*;

public abstract class IntEnumerationHelperBase implements IntEnumeration
{
  public abstract boolean hasMoreInts();
  public abstract int nextInt();

  public final boolean hasMoreElements()
    {return hasMoreInts();}

  public final Object nextElement()
    {return Integer.valueOf(nextInt());}
}
