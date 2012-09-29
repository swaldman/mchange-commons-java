/*
 * Distributed as part of mchange-commons-java v.0.2.3
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


package com.mchange.v1.util;

import java.util.Iterator;
import java.util.Map;


/**
 * @deprecated Oops! Doesn't conform to Map equals() / hashCode() contract!
 */
public final class MapUtils
{
    public static boolean equivalentDisregardingSort(Map a, Map b)
    {
	if (a.size() != b.size())
	    return false;

	for (Iterator ii = a.keySet().iterator(); ii.hasNext(); )
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
     */
    public static int hashContentsDisregardingSort(Map m)
    {
	int out = 0;
	for (Iterator ii = m.keySet().iterator(); ii.hasNext(); )
	    {
		Object key = ii.next();
		Object val = m.get( key );
		out ^= (key.hashCode() ^ val.hashCode());
	    }
	return out;
    }
}
