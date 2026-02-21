package com.mchange.v1.lang;

public final class BooleanUtils
{
    public static boolean parseBoolean(String str) throws IllegalArgumentException
    {
	if (str.equals("true"))
	    return true;
	else if (str.equals("false"))
	    return false;
	else
	    throw new IllegalArgumentException("\"str\" is neither \"true\" nor \"false\".");
    }

    private BooleanUtils()
    {}
}
