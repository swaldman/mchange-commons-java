package com.mchange.io;

import java.io.*;

/**
 * A generalized version of Enumeration, 
 * to be used by Enumerations whose retrieval 
 * of elements involves file or network access
 *
 * <P><TT>java.util.Enumeration</TT> ought to
 * extend a class like this, but it doesn't...
 */
public interface IOEnumeration
{
  public boolean hasMoreElements() throws IOException;
  public Object  nextElement()     throws IOException;
}
