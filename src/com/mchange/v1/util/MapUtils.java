package com.mchange.v1.util;

import java.util.Iterator;
import java.util.Map;


/**
 * @deprecated Oops! Doesn't conform to Map equals() / hashCode() contract!
 */
public final class MapUtils
{
    /**
     *  @deprecated Map.equals(Object) already compares by contents alone, independent
     *  of iteration order: a HashMap and a TreeMap holding the same mappings are equal
     *  to one another. Use it instead.
     *
     *  The two part ways only for a sorted map whose Comparator is inconsistent with
     *  equals. Relying on this method there would be unwise in any case, since such a
     *  map's own key lookups already disagree with equals.
     */
    @Deprecated
    public static boolean equivalentDisregardingSort(Map<?,?> a, Map<?,?> b)
    {
	if (a.size() != b.size())
	    return false;

	for (Iterator<?> ii = a.keySet().iterator(); ii.hasNext(); )
	    {
		Object key = ii.next();
		if (! a.get( key ).equals( b.get( key ) ))
		    return false;
	    }
	return true;
    }

    /**
     * finds a hash value which takes into account
     * the value of all elements, such that two maps
     * for which equivalentDisregardingSort(a, b) returns
     * true will hashContentsDisregardingSort() to the same value
     *
     * @deprecated Map.hashCode() is likewise defined by contents alone, independent of
     * iteration order, and pairs with Map.equals(Object). Use it instead.
     */
    @Deprecated
    public static int hashContentsDisregardingSort(Map<?,?> m)
    {
	int out = 0;
	for (Iterator<?> ii = m.keySet().iterator(); ii.hasNext(); )
	    {
		Object key = ii.next();
		Object val = m.get( key );
		out ^= (key.hashCode() ^ val.hashCode());
	    }
	return out;
    }
}
