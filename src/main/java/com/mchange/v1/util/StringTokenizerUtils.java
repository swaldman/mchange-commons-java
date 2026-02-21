package com.mchange.v1.util;

import java.util.StringTokenizer;

public final class StringTokenizerUtils
{
    public static String[] tokenizeToArray(String str, String delim, boolean returntokens)
    {
	StringTokenizer st = new StringTokenizer(str, delim, returntokens);
	String[] strings = new String[st.countTokens()];
	for (int i = 0; st.hasMoreTokens(); ++i)
	   strings[i] = st.nextToken();
	return strings;
    }

    public static String[] tokenizeToArray(String str, String delim)
    {return tokenizeToArray(str, delim, false);}

    public static String[] tokenizeToArray(String str)
    {return tokenizeToArray(str, " \t\r\n");}
}




