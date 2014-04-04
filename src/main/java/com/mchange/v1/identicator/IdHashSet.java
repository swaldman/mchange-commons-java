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
