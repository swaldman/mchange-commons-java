/*
 * Distributed as part of mchange-commons-java v.0.2.3.2
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

public class JoinedIterator implements Iterator
{
    Iterator[] its;
    Iterator   removeIterator = null;
    boolean    permit_removes;
    int        cur = 0;

    public JoinedIterator(Iterator[] its, boolean permit_removes)
    {
	this.its = its;
	this.permit_removes = permit_removes;
    }

    public boolean hasNext()
    {
	if (cur == its.length)
	    return false;
	else if (its[ cur ].hasNext())
	    return true;
	else
	    {
		++cur;
		return this.hasNext();
	    }
    }

    public Object next()
    {
	if (! this.hasNext())
	    throw new NoSuchElementException();

	removeIterator = its[cur];
	return removeIterator.next();
    }

    public void remove()
    {
	if (permit_removes)
	    {
		if (removeIterator != null)
		    {
			removeIterator.remove();
			removeIterator = null;
		    }
		else
		    throw new IllegalStateException("next() not called, or element already removed.");
	    }
	else
	    throw new UnsupportedOperationException();
    }
}
