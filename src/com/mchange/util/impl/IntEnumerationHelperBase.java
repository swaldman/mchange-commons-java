package com.mchange.util.impl;

import com.mchange.util.*;

public abstract class IntEnumerationHelperBase implements IntEnumeration
{
  @Override
  public abstract boolean hasMoreInts();
  @Override
  public abstract int nextInt();

  @Override
  public final boolean hasMoreElements()
    {return hasMoreInts();}

  @Override
  public final Object nextElement()
    {return Integer.valueOf(nextInt());}
}
