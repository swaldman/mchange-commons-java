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
