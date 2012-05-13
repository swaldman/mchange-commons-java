/*
 * Distributed as part of mchange-commonslib v.0.2
 *
 * Copyright (C) 2010 Machinery For Change, Inc.
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

public abstract class NumberedObjectCache
{
    ArrayList al = new ArrayList();
    
    public Object getObject(int num) throws Exception
    {
	Object out = null;
	int req_cap = num + 1;
	if (req_cap > al.size())
	    {
		al.ensureCapacity(req_cap * 2);
		for (int i = al.size(), end = req_cap * 2; i < end; ++i)
		    al.add(null);
		out = addToCache(num);
	    }
	else
	    {
		out = al.get(num);
		if (out == null)
		    out = addToCache(num);
	    }
	return out;
    }

    private Object addToCache(int num) throws Exception
    {
	Object out = findObject(num);
	al.set(num, out);
	return out;
    }

    protected abstract Object findObject(int num) throws Exception;
}
