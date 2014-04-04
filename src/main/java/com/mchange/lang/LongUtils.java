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

public class LongUtils
{
  private LongUtils()
    {}

  public static long longFromByteArray(byte[] bytes, int offset)
    {
      long out = 0;
      out |= ((long) ByteUtils.toUnsigned(bytes[offset + 0])) << 56;
      out |= ((long) ByteUtils.toUnsigned(bytes[offset + 1])) << 48;
      out |= ((long) ByteUtils.toUnsigned(bytes[offset + 2])) << 40;
      out |= ((long) ByteUtils.toUnsigned(bytes[offset + 3])) << 32;
      out |= ((long) ByteUtils.toUnsigned(bytes[offset + 4])) << 24;
      out |= ((long) ByteUtils.toUnsigned(bytes[offset + 5])) << 16;
      out |= ((long) ByteUtils.toUnsigned(bytes[offset + 6])) <<  8;
      out |= ((long) ByteUtils.toUnsigned(bytes[offset + 7])) <<  0;
      return out;
    }

  public static byte[] byteArrayFromLong(long l)
    {
      byte[] out = new byte[8];
      longIntoByteArray(l, 0, out);
      return out;
    }

  public static void longIntoByteArray(long l, int offset, byte[] bytes)
    {
      bytes[offset + 0] = (byte) ((l >>> 56) & 0xFF);
      bytes[offset + 1] = (byte) ((l >>> 48) & 0xFF);
      bytes[offset + 2] = (byte) ((l >>> 40) & 0xFF);
      bytes[offset + 3] = (byte) ((l >>> 32) & 0xFF);
      bytes[offset + 4] = (byte) ((l >>> 24) & 0xFF);
      bytes[offset + 5] = (byte) ((l >>> 16) & 0xFF);
      bytes[offset + 6] = (byte) ((l >>>  8) & 0xFF);
      bytes[offset + 7] = (byte) ((l >>>  0) & 0xFF);
    }

    public static int fullHashLong( long l )
    { return hashLong( l ); }

    public static int hashLong( long l )
    { return (int) l ^ (int) ( l >>> 32 ); }
}

