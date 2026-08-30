package com.mchange.io.impl;

import java.io.*;
import com.mchange.io.*;

public class LazyStringMemoryFileImpl extends LazyReadOnlyMemoryFileImpl implements StringMemoryFile
{
  private final static String DEFAULT_ENCODING;

  static
    {
      /* set the default encoding to the encosing specified in System props */
      /* or if none is set, use ISO Latin-1 (8859_1) */
      String check = (String) System.getProperty("file.encoding");
      DEFAULT_ENCODING = (check == null ? "8859_1" : check);
    }
      

  String encoding = null;
  String string   = null;

  public LazyStringMemoryFileImpl(File file)
    {super(file);}

  public LazyStringMemoryFileImpl(String fname)
    {super(fname);}

  public synchronized String asString(String enc) throws IOException, UnsupportedEncodingException
    {
      update();
      if (encoding != enc) 
	string = new String(bytes, enc);
      return string;
    }

  public String asString() throws IOException
    {
      try
	{return this.asString(DEFAULT_ENCODING);}
      catch (UnsupportedEncodingException e)
	{throw new InternalError("Default Encoding is not supported?!");}
    }

  /* should be called in sync'ed methods */
  void refreshBytes() throws IOException
    {
      super.refreshBytes();
      encoding = string = null;
    }
}
