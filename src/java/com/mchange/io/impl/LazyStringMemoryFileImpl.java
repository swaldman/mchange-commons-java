/*
 * Distributed as part of mchange-commons-java v.0.2.1
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


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
