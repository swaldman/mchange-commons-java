/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

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

