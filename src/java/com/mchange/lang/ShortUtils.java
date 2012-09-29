/*
 * Distributed as part of mchange-commons-java v.0.2.3
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


package com.mchange.lang;

public class ShortUtils
{
  public final static int UNSIGNED_MAX_VALUE = (Short.MAX_VALUE * 2) + 1;

  public static int shortFromByteArray(byte[] bytes, int offset)
    {
      int out = 0;
      out |= ((int) ByteUtils.toUnsigned(bytes[offset + 0])) <<  8;
      out |= ((int) ByteUtils.toUnsigned(bytes[offset + 1])) <<  0;
      return (short) out;
    }

  public static byte[] byteArrayFromShort(short s)
    {
      byte[] out = new byte[2];
      shortIntoByteArray(s, 0, out);
      return out;
    }

  public static void shortIntoByteArray(short s, int offset, byte[] bytes)
    {
      bytes[offset + 0] = (byte) ((s >>>  8) & 0xFF);
      bytes[offset + 1] = (byte) ((s >>>  0) & 0xFF);
    }

  public static int toUnsigned(short s)
    {return (s < 0 ? (UNSIGNED_MAX_VALUE + 1) + s : s);}

  private ShortUtils()
    {}
}
