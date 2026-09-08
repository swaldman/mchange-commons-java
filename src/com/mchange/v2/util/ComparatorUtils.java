package com.mchange.v2.util;

import java.util.Comparator;

public final class ComparatorUtils
{
    public static <T> Comparator<T> reverse( final Comparator<T> c )
    { 
	return new Comparator<T>()
	    {
		@Override
		public int compare( T a, T b )
		{ return -c.compare( a, b ); }
	    };
    }
}
