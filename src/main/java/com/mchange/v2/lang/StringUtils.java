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

import java.util.regex.*;
import java.io.UnsupportedEncodingException;

/**
 *  requires JDK1.4+ (try com.mchange.lang.StringUtils for older JVMs)
 */
public final class StringUtils
{
    final static Pattern COMMA_SEP_TRIM_REGEX;
    final static Pattern COMMA_SEP_NO_TRIM_REGEX;

    static
    {
	try
	    {
		COMMA_SEP_TRIM_REGEX = Pattern.compile("\\s*\\,\\s*");
		COMMA_SEP_NO_TRIM_REGEX = Pattern.compile("\\,");
	    }
	catch ( PatternSyntaxException e )
	    { 
		e.printStackTrace(); 
		throw new InternalError( e.toString() );
	    }
    }
    

    public final static String[] EMPTY_STRING_ARRAY = new String[0];

    public static String normalString(String s)
    { return nonEmptyTrimmedOrNull(s); }

    public static boolean nonEmptyString(String s)
    {return (s != null && s.length() > 0);}

    public static boolean nonWhitespaceString(String s)
    {return (s != null && s.trim().length() > 0);}

    public static String nonEmptyOrNull(String s)
    {return ( nonEmptyString(s) ? s : null );}

    public static String nonNullOrBlank(String s)
    {return ( s!= null ? s : "" );}

    public static String nonEmptyTrimmedOrNull(String s)
    {
        String out = s;
        if (out != null)
            {
                out = out.trim();
                out = (out.length() > 0 ? out : null);
            }
        return out;
    }

    public static byte[] getUTF8Bytes( String s )
    {
	try
	    { return s.getBytes( "UTF8" ); }
	catch (UnsupportedEncodingException e)
	    {
		e.printStackTrace();
		throw new InternalError("UTF8 is an unsupported encoding?!?");
	    }
    }

    public static String[] splitCommaSeparated(String commaSep, boolean trim)
    {
	Pattern pattern = trim ? COMMA_SEP_TRIM_REGEX : COMMA_SEP_NO_TRIM_REGEX;
	return pattern.split( commaSep );
    }
}


