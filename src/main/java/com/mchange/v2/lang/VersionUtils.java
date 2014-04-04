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

package com.mchange.v2.lang;

import com.mchange.v2.log.*;
import com.mchange.v1.util.StringTokenizerUtils;

public final class VersionUtils
{
    private final static MLogger logger = MLog.getLogger( VersionUtils.class );

    private final static int[] DFLT_VERSION_ARRAY = {1,1};

    private final static int[] JDK_VERSION_ARRAY;
    private final static int JDK_VERSION; //two digit int... 10 for 1.0, 11 for 1.1, etc.

    private final static Integer NUM_BITS;

    static
    {
	String vstr = System.getProperty( "java.version" );
	int[] v;
	if (vstr == null)
	    {
		if (logger.isLoggable( MLevel.WARNING ))
		    logger.warning("Could not find java.version System property. Defaulting to JDK 1.1");
		v = DFLT_VERSION_ARRAY;
	    }
	else
	    { 
		try { v = extractVersionNumberArray( vstr ); }
		catch ( NumberFormatException e )
		    {
			if (logger.isLoggable( MLevel.WARNING ))
			    logger.warning("java.version ''" + vstr + "'' could not be parsed. Defaulting to JDK 1.1.");
			v = DFLT_VERSION_ARRAY;
		    }
	    }
	int jdkv = 0;
	if (v.length > 0)
	    jdkv += (v[0] * 10);
	if (v.length > 1)
	    jdkv += (v[1]);

	JDK_VERSION_ARRAY = v;
	JDK_VERSION = jdkv;

	//System.err.println( JDK_VERSION );

	Integer tmpNumBits;
	try
	    {
		String numBitsStr = System.getProperty("sun.arch.data.model");
		if (numBitsStr == null)
		    tmpNumBits = null;
		else
		    tmpNumBits = new Integer( numBitsStr );
	    }
	catch (Exception e)
	    {
		tmpNumBits = null;
	    }

	if (tmpNumBits == null || tmpNumBits.intValue() == 32 || tmpNumBits.intValue() == 64)
	    NUM_BITS = tmpNumBits;
	else
	    {
		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.warning("Determined a surprising jvmNumerOfBits: " + tmpNumBits + 
				   ". Setting jvmNumberOfBits to unknown (null).");
		NUM_BITS = null;
	    }
    }

    /**
     *  @return null if unknown, 
     *          an Integer (as of 2006 always 32 or 64)
     *          otherwise
     */
    public static Integer jvmNumberOfBits()
    { return NUM_BITS; }

    public static boolean isJavaVersion10()
    { return (JDK_VERSION == 10); }

    public static boolean isJavaVersion11()
    { return (JDK_VERSION == 11); }

    public static boolean isJavaVersion12()
    { return (JDK_VERSION == 12); }

    public static boolean isJavaVersion13()
    { return (JDK_VERSION == 13); }

    public static boolean isJavaVersion14()
    { return (JDK_VERSION == 14); }

    public static boolean isJavaVersion15()
    { return (JDK_VERSION == 15); }

    public static boolean isAtLeastJavaVersion10()
    { return (JDK_VERSION >= 10); }

    public static boolean isAtLeastJavaVersion11()
    { return (JDK_VERSION >= 11); }

    public static boolean isAtLeastJavaVersion12()
    { return (JDK_VERSION >= 12); }

    public static boolean isAtLeastJavaVersion13()
    { return (JDK_VERSION >= 13); }

    public static boolean isAtLeastJavaVersion14()
    { return (JDK_VERSION >= 14); }

    public static boolean isAtLeastJavaVersion15()
    { return (JDK_VERSION >= 15); }
    
    public static boolean isAtLeastJavaVersion16()
    { return (JDK_VERSION >= 16); }
    
    public static boolean isAtLeastJavaVersion17()
    { return (JDK_VERSION >= 17); }
    
    public static int[] extractVersionNumberArray(String versionString)
        throws NumberFormatException
    { return extractVersionNumberArray( versionString, versionString.split("\\D+") ); }

    public static int[] extractVersionNumberArray(String versionString, String delims)
	throws NumberFormatException
    {
	String[] intStrs = StringTokenizerUtils.tokenizeToArray( versionString, delims, false );
	return extractVersionNumberArray( versionString, intStrs );
    }

    private static int[] extractVersionNumberArray(String versionString, String[] intStrs)
	throws NumberFormatException
    {
	int len = intStrs.length;
	int[] out = new int[ len ];
	for (int i = 0; i < len; ++i)
	    {
		try
		    {
			out[i] = Integer.parseInt( intStrs[i] );
		    }
		catch (NumberFormatException e)
		    {
			if (i <= 1) //we don't even have the major version, e.g. 1.2
			    throw e; // just bail
			else //we'll make do with what we have
			    {
				if (logger.isLoggable(MLevel.INFO))
				    logger.log(MLevel.INFO, 
					       "JVM version string (" +
					       versionString + 
					       ") contains non-integral component (" + intStrs[i] + 
					       "). Using precending components only to resolve JVM version.");
				
				int[] goodEnough = new int[i];
				System.arraycopy(out, 0, goodEnough, 0, i);
				out = goodEnough;
				break;
			    }
		    }
	    }
	return out;
    }

    public boolean prefixMatches( int[] pfx, int[] fullVersion )
    {
	if (pfx.length > fullVersion.length)
	    return false;
	else
	    {
		for (int i = 0, len = pfx.length; i < len; ++i)
		    if (pfx[i] != fullVersion[i])
			return false;
		return true;
	    }
    }

    public static int lexicalCompareVersionNumberArrays(int[] a, int[] b)
    {
	int alen = a.length;
	int blen = b.length;
	for (int i = 0; i < alen; ++i)
	    {
		if (i == blen)
		    return 1; //a is larger if they are the same to a point, but a has an extra version number
		else if (a[i] > b[i])
		    return 1;
		else if (a[i] < b[i])
		    return -1;
	    }
	if (blen > alen)
	    return -1; //a is smaller if they are the same to a point, but b has an extra version number
	else
	    return 0;
    }
}
