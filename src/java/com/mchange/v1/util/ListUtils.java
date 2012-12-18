/*
 * Distributed as part of mchange-commons-java v.0.2.3.3
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

import java.util.*;

public final class ListUtils
{
    public static List oneElementUnmodifiableList(final Object elem)
    {
	return new AbstractList()
	    {
		public Iterator iterator()
		{ return IteratorUtils.oneElementUnmodifiableIterator( elem ); }
		
		public int size() { return 1; }
		
		public boolean isEmpty()
		{ return false; }
		
		public boolean contains(Object o) 
		{ return o == elem; }

		public Object get(int index)
		{
		    if (index != 0)
			throw new IndexOutOfBoundsException("One element list has no element index " + 
							    index);
		    else
			return elem;
		}
	    };
    }

    //we could improve performance here by delegating to a method
    //that didn't recheck size...
    public static boolean equivalent(List a, List b)
    {
	if (a.size() != b.size())
	    return false;
	else
	    {
		Iterator ii = a.iterator();
		Iterator jj = b.iterator();
		return IteratorUtils.equivalent(ii, jj);
	    }
    }

    /**
     * finds a hash value which takes into account
     * the value of all elements, such that two sets
     * for which equivalent(a, b) returns
     * true will hashContents() to the same value
     */
    public static int hashContents(List l)
    {
	int out = 0;
	int count = 0;
	for (Iterator ii = l.iterator(); ii.hasNext(); ++count)
	    {
		Object o = ii.next();
		if (o != null) out ^= (o.hashCode() ^ count);
	    }
	return out;
    }
}




