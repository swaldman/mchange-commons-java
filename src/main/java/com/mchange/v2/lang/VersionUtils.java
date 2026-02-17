/*
 * Distributed as part of mchange-commons-java 0.2.11
 *
 * Copyright (C) 2015 Machinery For Change, Inc.
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
    private final static int JDK_VERSION; //1.x => x, otherwise y.x => y

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
			    logger.warning("java.version \"" + vstr + "\" could not be parsed. Defaulting to JDK 1.1.");
			v = DFLT_VERSION_ARRAY;
		    }
	    }
	if ( v.length == 0 ) {
	    if (logger.isLoggable( MLevel.WARNING ))
		logger.warning("java.version \"" + vstr + "\" is prefixed by no integral elements. Defaulting to JDK 1.1.");
	    v = DFLT_VERSION_ARRAY;
	}
	
	int jdkv;
	if (v[0] > 1)
	{
	    jdkv = v[0];
	}
	else if (v[0] == 1)
	{
	    if ( v.length > 1 ) jdkv = v[1];
	    else
	    {
		if (logger.isLoggable( MLevel.WARNING ))
		    logger.warning("java.version \"" + vstr + "\" looks like a 1.x style bargain, but the second element cannot be parsed. Defaulting to JDK 1.1.");
		jdkv = 1;
	    }
	}
	else
	{
	    if (logger.isLoggable( MLevel.WARNING ))
		logger.warning("Illegal java.version \"" + vstr + "\". Defaulting to JDK 1.1.");
	    jdkv = 1;
	}
			  

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
		    tmpNumBits = Integer.valueOf( numBitsStr );
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

    public static boolean isJavaVersion1_0()
    { return (JDK_VERSION == 0); }

    public static boolean isJavaVersion1_1()
    { return (JDK_VERSION == 1); }

    public static boolean isJavaVersion1_2()
    { return (JDK_VERSION == 2); }

    public static boolean isJavaVersion1_3()
    { return (JDK_VERSION == 3); }

    public static boolean isJavaVersion1_4()
    { return (JDK_VERSION == 4); }

    public static boolean isJavaVersion1_5()
    { return (JDK_VERSION == 5); }

    public static boolean isJavaVersion1_6()
    { return (JDK_VERSION == 6); }

    public static boolean isJavaVersion1_7()
    { return (JDK_VERSION == 7); }

    public static boolean isJavaVersion1_8()
    { return (JDK_VERSION == 8); }

    public static boolean isJavaVersion1_9()
    { return (JDK_VERSION == 9); }

    public static boolean isJava5()
    { return (JDK_VERSION == 5); }

    public static boolean isJava6()
    { return (JDK_VERSION == 6); }

    public static boolean isJava7()
    { return (JDK_VERSION == 7); }

    public static boolean isJava8()
    { return (JDK_VERSION == 8); }

    public static boolean isJava9()
    { return (JDK_VERSION == 9); }

    public static boolean isJava10()
    { return (JDK_VERSION == 10); }

    public static boolean isJava11()
    { return (JDK_VERSION == 11); }

    public static boolean isJava12()
    { return (JDK_VERSION == 12); }

    public static boolean isJava13()
    { return (JDK_VERSION == 13); }

    public static boolean isAtLeastJavaVersion1_0()
    { return (JDK_VERSION >= 0); }

    public static boolean isAtLeastJavaVersion1_1()
    { return (JDK_VERSION >= 1); }

    public static boolean isAtLeastJavaVersion1_2()
    { return (JDK_VERSION >= 2); }

    public static boolean isAtLeastJavaVersion1_3()
    { return (JDK_VERSION >= 3); }

    public static boolean isAtLeastJavaVersion1_4()
    { return (JDK_VERSION >= 4); }

    public static boolean isAtLeastJavaVersion1_5()
    { return (JDK_VERSION >= 5); }
    
    public static boolean isAtLeastJavaVersion1_6()
    { return (JDK_VERSION >= 6); }
    
    public static boolean isAtLeastJavaVersion1_7()
    { return (JDK_VERSION >= 7); }

    public static boolean isAtLeastJavaVersion1_8()
    { return (JDK_VERSION >= 8); }

    public static boolean isAtLeastJavaVersion1_9()
    { return (JDK_VERSION >= 9); }

    public static boolean isAtLeastJava5()
    { return (JDK_VERSION >= 5); }
    
    public static boolean isAtLeastJava6()
    { return (JDK_VERSION >= 6); }
    
    public static boolean isAtLeastJava7()
    { return (JDK_VERSION >= 7); }

    public static boolean isAtLeastJava8()
    { return (JDK_VERSION >= 8); }

    public static boolean isAtLeastJava9()
    { return (JDK_VERSION >= 9); }

    public static boolean isAtLeastJava10()
    { return (JDK_VERSION >= 10); }

    public static boolean isAtLeastJava11()
    { return (JDK_VERSION >= 11); }

    public static boolean isAtLeastJava12()
    { return (JDK_VERSION >= 12); }

    public static boolean isAtLeastJava13()
    { return (JDK_VERSION >= 13); }

    /** @deprecated ambiguous between "one dot zero" and "ten" */ 
    public static boolean isJavaVersion10()
    { return (JDK_VERSION == 0); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isJavaVersion11()
    { return (JDK_VERSION == 1); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isJavaVersion12()
    { return (JDK_VERSION == 2); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isJavaVersion13()
    { return (JDK_VERSION == 3); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isJavaVersion14()
    { return (JDK_VERSION == 4); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isJavaVersion15()
    { return (JDK_VERSION == 5); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isAtLeastJavaVersion10()
    { return (JDK_VERSION >= 0); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isAtLeastJavaVersion11()
    { return (JDK_VERSION >= 1); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isAtLeastJavaVersion12()
    { return (JDK_VERSION >= 2); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isAtLeastJavaVersion13()
    { return (JDK_VERSION >= 3); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isAtLeastJavaVersion14()
    { return (JDK_VERSION >= 4); }

    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isAtLeastJavaVersion15()
    { return (JDK_VERSION >= 5); }
    
    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isAtLeastJavaVersion16()
    { return (JDK_VERSION >= 6); }
    
    /** @deprecated ambiguous between "one dot x" and "x" */ 
    public static boolean isAtLeastJavaVersion17()
    { return (JDK_VERSION >= 7); }
    
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
			if (i == 0 || i == 1 && out[0] < 5) //we don't even have the major version, e.g. 1.2, and we are not late enough for versions sometime to look like "Java 5"
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

    /*
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
    */
}
