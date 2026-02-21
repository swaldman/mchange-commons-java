package com.mchange.v1.util;

import com.mchange.util.AssertException;

public class DebugUtils
{
    private DebugUtils() {}
    
    public static void myAssert(boolean bool)
    {if (!bool) throw new AssertException();}
    
    public static void myAssert(boolean bool, String message)
    {if (!bool) throw new AssertException(message);}
}

