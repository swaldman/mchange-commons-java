package com.mchange.io.impl;

import java.io.*;
import com.mchange.io.*;

public abstract class IOStringEnumerationHelperBase implements IOStringEnumeration
{
  @Override
  public abstract boolean hasMoreStrings() throws IOException;
  @Override
  public abstract String  nextString()     throws IOException;

  @Override
  public final boolean hasMoreElements()   throws IOException
    {return hasMoreStrings();}

  @Override
  public final Object nextElement()        throws IOException
    {return nextString();}
}
