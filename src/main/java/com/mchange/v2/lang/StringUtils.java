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


