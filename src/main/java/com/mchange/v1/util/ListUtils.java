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




