package com.mchange.util.impl;

import com.mchange.util.*;

public abstract class StringEnumerationHelperBase implements StringEnumeration
{
  public abstract boolean hasMoreStrings();
  public abstract String  nextString();

  public final boolean hasMoreElements()
    {return hasMoreStrings();}

  public final Object nextElement()
    {return nextString();}
}
