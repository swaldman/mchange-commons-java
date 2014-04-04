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

public final class IntegerUtils
{
    public final static long UNSIGNED_MAX_VALUE = (Integer.MAX_VALUE * 2) + 1;

    public static int parseInt(String intStr, int dflt)
    {
	if (intStr == null)
	    return dflt;

	try
	    {return Integer.parseInt(intStr);}
	catch (NumberFormatException e)
	    {return dflt;}
    }

    public static int parseInt(String intStr, int radix, int dflt)
    {
	if (intStr == null)
	    return dflt;

	try
	    {return Integer.parseInt(intStr, radix);}
	catch (NumberFormatException e)
	    {return dflt;}
    }

    public static int intFromByteArray(byte[] bytes, int offset)
    {
	int out = 0;
	out |= ((int) ByteUtils.toUnsigned(bytes[offset + 0])) << 24;
	out |= ((int) ByteUtils.toUnsigned(bytes[offset + 1])) << 16;
	out |= ((int) ByteUtils.toUnsigned(bytes[offset + 2])) <<  8;
	out |= ((int) ByteUtils.toUnsigned(bytes[offset + 3])) <<  0;
	return out;
    }

    public static byte[] byteArrayFromInt(int i)
    {
	byte[] out = new byte[4];
	intIntoByteArray(i, 0, out);
	return out;
    }

    public static void intIntoByteArray(int i, int offset, byte[] bytes)
    {
	bytes[offset + 0] = (byte) ((i >>> 24) & 0xFF);
	bytes[offset + 1] = (byte) ((i >>> 16) & 0xFF);
	bytes[offset + 2] = (byte) ((i >>>  8) & 0xFF);
	bytes[offset + 3] = (byte) ((i >>>  0) & 0xFF);
    }

    public static long toUnsigned(int i)
    {return (i < 0 ? (UNSIGNED_MAX_VALUE + 1) + i : i);}

    private IntegerUtils()
    {}
}
