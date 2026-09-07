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
		@Override
		public Iterator iterator()
		{ return IteratorUtils.oneElementUnmodifiableIterator( elem ); }

		@Override
		public int size() { return 1; }

		@Override
		public boolean isEmpty()
		{ return false; }

		@Override
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

    /**
     *  @deprecated Set.equals(Object) already compares by contents alone, independent
     *  of iteration order: a HashSet and a TreeSet holding the same elements are equal
     *  to one another. Use it instead.
     *
     *  The two part ways only for a sorted set whose Comparator is inconsistent with
     *  equals. Relying on this method there would be unwise in any case, since such a
     *  set's own membership tests already disagree with equals.
     */
    @Deprecated
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
     *
     * @deprecated Set.hashCode() is likewise defined by contents alone, independent of
     * iteration order, and pairs with Set.equals(Object). Use it instead.
     */
    @Deprecated
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

