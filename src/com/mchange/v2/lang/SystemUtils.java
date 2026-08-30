package com.mchange.v2.lang;

import java.util.Collections;
import java.util.Map;
import java.util.regex.*;

// NOTE: Don't ever use com.mchange.v2.log logging in this class, as log initialization makes use of it

/**
 *  java.lang.System related utils. At present, just some utilities for replacing
 *  parts of Strings with System properties or environment variables
 */
public final class SystemUtils
{
    private final static Pattern REPLACE_ME_REGEX;
    private final static Pattern UNESCAPE_ME_REGEX;

    static
    {
	REPLACE_ME_REGEX = Pattern.compile("(?<!\\$)\\$\\{\\s*(.+?)\\s*\\}");
	UNESCAPE_ME_REGEX = Pattern.compile("\\$\\$\\{\\s*(.+?)\\s*\\}");
    }

    private static String _unescape( String replaced )
    {
	Matcher m = UNESCAPE_ME_REGEX.matcher(replaced);
	StringBuffer sb = new StringBuffer();
	while (m.find())
	{
	    // we have to escape the remaining '$' so matcher does not
	    // interpret it as signifying a group name
	    String replacement = '\\' + m.group(0).substring(1); 
	    m.appendReplacement(sb, replacement);
	}
	m.appendTail(sb);
	return sb.toString();
    }

    private static String _mapReplace( String source, Map<String,String> replacements )
    {
	Matcher m = REPLACE_ME_REGEX.matcher(source);
	StringBuffer sb = new StringBuffer();
	while (m.find())
	{
	    String replacement = replacements.get( m.group(1) );
	    if ( replacement != null )
	    { m.appendReplacement(sb, replacement); }
	}
	m.appendTail(sb);
	return sb.toString();
    }

    private static Map<String,String> propsMap()
    { return Collections.checkedMap( (Map) System.getProperties(), String.class, String.class ); }

    /**
     *  Use $${....} as escapes for ${....}
     */
    public static String mapReplace( String source, Map<String,String> replacements )
    { return _unescape( _mapReplace( source, replacements ) );  }

    /**
     *  Use $${....} as escapes for ${....}
     */
    @SuppressWarnings("unchecked")
    public static String sysPropsReplace( String source )
    { return mapReplace( source, propsMap() ); }

    /**
     *  Use $${....} as escapes for ${....}
     */
    public static String envReplace( String source )
    { return mapReplace( source, System.getenv() ); }

    /**
     *  Use $${....} as escapes for ${....}
     */
    public static String sysPropsEnvReplace( String source )
    {
	String halfway = _mapReplace( source, propsMap() );
	return envReplace( halfway );
    }

    private SystemUtils()
    {}
}
