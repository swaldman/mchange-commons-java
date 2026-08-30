package com.mchange.v2.util;

import java.util.Comparator;

public final class ComparatorUtils
{
    public static Comparator reverse( final Comparator c )
    { 
	return new Comparator()
	    {
		public int compare( Object a, Object b )
		{ return -c.compare( a, b ); }
	    };
    }
}
