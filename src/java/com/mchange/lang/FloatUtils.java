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

public final class FloatUtils
{
    final static boolean DEBUG = true;

    //yukky kludge, 'cuz Float.parseFloat did not exist in Java 1.1, 
    //but it is better to use this method (rather than the GC-straining
    //workaround) where possible.
    private static FParser fParser;

    static
    {
	try
	    {
		fParser = new J12FParser();
		fParser.parseFloat("0.1");
	    }
	catch (NoSuchMethodError e)
	    {
		if (DEBUG)
		    System.err.println("com.mchange.lang.FloatUtils: reconfiguring for Java 1.1 environment");
		fParser = new J11FParser();
	    }
    }

    public static byte[] byteArrayFromFloat( float f )
    {
	int i = Float.floatToIntBits( f );
	return IntegerUtils.byteArrayFromInt(i);
    }

    public static float floatFromByteArray( byte[] b, int offset )
    {
	int i = IntegerUtils.intFromByteArray( b, offset );
	return Float.intBitsToFloat(i);
    }

    public static float parseFloat(String fStr, float dflt)
    {
	if (fStr == null)
	    return dflt;

	try
	    {return fParser.parseFloat(fStr);}
	catch (NumberFormatException e)
	    {return dflt;}
    }

    /**
     * useful in Java1.1 environments, where Float.parseFloat is not defined...
     */
    public static float parseFloat(String fStr) throws NumberFormatException
    {return fParser.parseFloat(fStr);}

    public static String floatToString(float f, int precision)
    {
	boolean negative = f < 0;
	f = (negative ? -f : f);

	long whole_rep = Math.round(f * Math.pow(10, -precision));

	String wholeRepStr = String.valueOf(whole_rep);
	if (whole_rep == 0)
	    return wholeRepStr;

	int whole_len = wholeRepStr.length();
	int pre_len   = whole_len + precision;
				  
	StringBuffer sb = new StringBuffer(32);
	if (negative) sb.append('-');

	if (pre_len <= 0)
	    {
		sb.append("0.");
		for (int i = 0; i < -pre_len; ++i)
		    sb.append('0');
		sb.append(wholeRepStr);
	    }
	else
	    {
// 		System.out.println("wholeRepStr: " + wholeRepStr);
// 		System.out.println("pre_len: " + pre_len);
		sb.append(wholeRepStr.substring(0, Math.min(pre_len, whole_len)));
		if (pre_len < whole_len)
		    {
			sb.append('.');
			sb.append(wholeRepStr.substring(pre_len));
		    }
		else if (pre_len > whole_len)
		    {
			for (int i = 0, len = pre_len - whole_len; i < len; ++i)
			    sb.append('0');
		    }
	    }
	return sb.toString();
    }

    interface FParser
    {
	public float parseFloat(String fStr) throws NumberFormatException;
    }

    static class J12FParser implements FParser
    {
	public float parseFloat(String fStr) throws NumberFormatException
	{return Float.parseFloat(fStr);}
    }

    static class J11FParser implements FParser
    {
	public float parseFloat(String fStr) throws NumberFormatException
	{return new Float(fStr).floatValue();}
    }	

}
