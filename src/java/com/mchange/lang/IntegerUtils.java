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
