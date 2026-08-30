package com.mchange.io.impl;

import java.io.*;
import com.mchange.io.*;

public abstract class IOStringEnumerationHelperBase implements IOStringEnumeration
{
  public abstract boolean hasMoreStrings() throws IOException;
  public abstract String  nextString()     throws IOException;

  public final boolean hasMoreElements()   throws IOException
    {return hasMoreStrings();}

  public final Object nextElement()        throws IOException
    {return nextString();}
}
