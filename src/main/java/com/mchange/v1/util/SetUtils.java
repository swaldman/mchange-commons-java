package com.mchange.v1.util;

import java.util.Iterator;
import java.util.Set;
import java.util.AbstractSet;
import java.util.HashSet;

public final class SetUtils
{
    public static Set oneElementUnmodifiableSet(final Object elem)
    {
	return new AbstractSet()
	    {
		public Iterator iterator()
		{ return IteratorUtils.oneElementUnmodifiableIterator( elem ); }

		public int size() { return 1; }

		public boolean isEmpty()
		{ return false; }

		public boolean contains(Object o) 
		{ return o == elem; }

	    };
    }

    public static Set setFromArray(Object[] array)
    {
	HashSet out = new HashSet();
	for (int i = 0, len = array.length; i < len; ++i)
	    out.add( array[i] );
	return out;
    }

    public static boolean equivalentDisregardingSort(Set a, Set b)
    {
	return 
	    a.containsAll( b ) &&
	    b.containsAll( a );
    }

    /**
     * finds a hash value which takes into account
     * the value of all elements, such that two sets
     * for which equivalentDisregardingSort(a, b) returns
     * true will hashContentsDisregardingSort() to the same value
     */
    public static int hashContentsDisregardingSort(Set s)
    {
	int out = 0;
	for (Iterator ii = s.iterator(); ii.hasNext(); )
	    {
		Object o = ii.next();
		if (o != null) out ^= o.hashCode();
	    }
	return out;
    }
}

