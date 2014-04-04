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

package com.mchange.lang;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.IOException;

public final class ByteUtils
{
  public final static short UNSIGNED_MAX_VALUE = (Byte.MAX_VALUE * 2) + 1;

  public static short toUnsigned(byte b)
    {return (short) (b < 0 ? (UNSIGNED_MAX_VALUE + 1) + b : b);}

  public static String toHexAscii(byte b)
    {
      StringWriter sw = new StringWriter(2);
      addHexAscii(b, sw);
      return sw.toString();
    }

  public static String toHexAscii(byte[] bytes)
    {
      int len = bytes.length;
      StringWriter sw = new StringWriter(len * 2);
      for (int i = 0; i < len; ++i)
	addHexAscii(bytes[i], sw);
      return sw.toString();
    }

  public static byte[] fromHexAscii(String s) throws NumberFormatException
    {
      try
	{
	  int len = s.length();
	  if ((len % 2) != 0)
	    throw new NumberFormatException("Hex ascii must be exactly two digits per byte.");
	  
	  int out_len = len / 2;
	  byte[] out = new byte[out_len];
	  int i = 0;
	  StringReader sr = new StringReader(s); 
	  while (i < out_len)
	    {
	      int val = (16 * fromHexDigit(sr.read())) + fromHexDigit(sr.read()); 
	      out[i++] = (byte) val;
	    }
	  return out;
	}
      catch (IOException e)
	{throw new InternalError("IOException reading from StringReader?!?!");}
    }

  static void addHexAscii(byte b, StringWriter sw)
    {
      short ub = toUnsigned(b);
      int h1 = ub / 16;
      int h2 = ub % 16;
      sw.write(toHexDigit(h1));
      sw.write(toHexDigit(h2));
    }

  private static int fromHexDigit(int c) throws NumberFormatException
    {
      if (c >= 0x30 && c < 0x3A)
	return c - 0x30;
      else if (c >= 0x41 && c < 0x47)
	return c - 0x37;
      else if (c >= 0x61 && c < 0x67)
	return c - 0x57;
      else 
	throw new NumberFormatException('\'' + c + "' is not a valid hexadecimal digit.");
    }

  /* note: we do no arg. checking, because     */
  /* we only ever call this from addHexAscii() */
  /* above, and we are sure the args are okay  */
  private static char toHexDigit(int h)
    {
	char out;
	if (h <= 9) out = (char) (h + 0x30);
	else out = (char) (h + 0x37);
	//System.err.println(h + ": " + out);
	return out;
    }

  private ByteUtils()
    {}
}
