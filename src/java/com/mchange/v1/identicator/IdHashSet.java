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


package com.mchange.v1.identicator;

import java.util.*;
import com.mchange.v1.util.WrapperIterator;

public class IdHashSet extends AbstractSet implements Set
{
    HashSet     inner;
    Identicator id;

    private IdHashSet(HashSet inner, Identicator id)
    {
	this.inner = inner;
	this.id = id;
    }

    public IdHashSet(Identicator id)
    { this( new HashSet(), id ); }

    public IdHashSet(Collection c, Identicator id) 
    { this( new HashSet(2 * c.size()), id ); } 

    public IdHashSet(int initialCapacity, float loadFactor, Identicator id) 
    { this( new HashSet( initialCapacity, loadFactor ), id ); }

    public IdHashSet(int initialCapacity, Identicator id) 
    { this(new HashSet( initialCapacity, 0.75f ), id); }

    public Iterator iterator()
    {
	return new WrapperIterator(inner.iterator(), true)
	    {
		protected Object transformObject(Object o)
		{
		    IdHashKey idKey = (IdHashKey) o;
		    return idKey.getKeyObj();
		}
	    };
    }

    public int size()
    { return inner.size(); }

    public boolean contains(Object o)
    { return inner.contains( createKey( o ) ); }

    public boolean add(Object o)
    { return inner.add( createKey( o ) ); }

    public boolean remove(Object o)
    { return inner.remove( createKey( o ) ); }

    public void clear()
    { inner.clear(); }

    private IdHashKey createKey(Object o)
    { return new StrongIdHashKey( o, id ); }
}
