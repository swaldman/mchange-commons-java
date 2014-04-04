/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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
