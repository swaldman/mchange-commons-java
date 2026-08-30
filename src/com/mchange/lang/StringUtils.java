package com.mchange.lang;

import java.io.UnsupportedEncodingException;

/**
 * @deprecated use com.mchange.v2.lang.StringUtils (JDK 1.4+)
 */
public final class StringUtils
{
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
}


