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
import com.mchange.v1.util.*;

public class IdList implements List
{
    Identicator id;
    List inner;

    public IdList(Identicator id, List inner)
    {
	this.id = id;
	this.inner = inner;
    }

    public int size()
    { return inner.size(); }

    public boolean isEmpty()
    { return inner.isEmpty(); }

    public boolean contains(Object o)
    {
	IdHashKey wrappedO = new StrongIdHashKey(o, id);
	return inner.contains(o);
    }

    public Iterator iterator()
    {
	return new WrapperIterator( inner.iterator(), true )
	    {
		protected Object transformObject(Object o)
		{
		    if (o instanceof IdHashKey)
			{
			    IdHashKey ik = (IdHashKey) o;
			    return ik.getKeyObj();
			}
		    else //we expect that o is null then... 
			return o; 
		}
	    };
    }

    public Object[] toArray()
    { return this.toArray( new Object[ this.size() ] ); }

    public Object[] toArray(Object[] space)
    { return IteratorUtils.toArray( this.iterator(), this.size(), space ); }

    public boolean add(Object o)
    { return inner.add( new StrongIdHashKey( o, id ) ); }

    public boolean remove(Object o)
    { return inner.remove( new StrongIdHashKey( o, id ) ); }

    public boolean containsAll(Collection c)
    {
	Iterator ii = c.iterator();
	while (ii.hasNext())
	    {
		IdHashKey test = new StrongIdHashKey( ii.next(), id );
		if (! inner.contains( test ))
		    return false;
	    }
	return true;
    }

    public boolean addAll(Collection c)
    {
	Iterator ii = c.iterator();
	boolean changed = false;
	while (ii.hasNext())
	    {
		IdHashKey ik = new StrongIdHashKey( ii.next(), id );
		changed |= inner.add( ik );
	    }
	return changed;
    }

    public boolean addAll(int i, Collection c)
    {
	Iterator ii = c.iterator();
	while (ii.hasNext())
	    {
		IdHashKey ik = new StrongIdHashKey( ii.next(), id );
		inner.add( i, ik );
		++i;
	    }
	return (c.size() > 0);
    }

    public boolean removeAll(Collection c)
    {
	Iterator ii = c.iterator();
	boolean changed = false;
	while (ii.hasNext())
	    {
		IdHashKey ik = new StrongIdHashKey( ii.next(), id );
		changed |= inner.remove( ik );
	    }
	return changed;
    }

    public boolean retainAll(Collection c)
    {
	Iterator ii = inner.iterator();
	boolean changed = false;
	while (ii.hasNext())
	    {
		IdHashKey ours = (IdHashKey) ii.next();
		if (! c.contains( ours.getKeyObj() ))
		    {
			inner.remove( ours );
			changed = true;
		    }
	    }
	return changed;
    }

    public void clear()
    { inner.clear(); }

    //TODO: should I make some version of this that pays attention to identicator?
    public boolean equals(Object o)
    { 
	if (o instanceof List)
	    return ListUtils.equivalent( this, (List) o );
	else
	    return false;
    }

    public int hashCode()
    { return ListUtils.hashContents( this ); }

    public Object get(int i)
    { return ((IdHashKey) inner.get(i)).getKeyObj(); }

    public Object set(int i, Object o)
    {
	IdHashKey ik = (IdHashKey) inner.set(  i, new StrongIdHashKey( o, id ) );
	return ik.getKeyObj();
    }

    public void add(int i, Object o)
    {
	inner.add(  i, new StrongIdHashKey( o, id ) );
    }

    public Object remove(int i)
    {
	IdHashKey ik = (IdHashKey) inner.remove(i);
	return (ik == null ? null : ik.getKeyObj());
    }

    public int indexOf(Object o)
    { return inner.indexOf( new StrongIdHashKey( o, id ) ); }

    public int lastIndexOf(Object o)
    { return inner.lastIndexOf( new StrongIdHashKey( o, id ) ); }

    //TODO: make a more efficient implementation...
    public ListIterator listIterator()
    { return new LinkedList(this).listIterator(); }

    //TODO: make a more efficient implementation...
    public ListIterator listIterator(int i)
    { return new LinkedList(this).listIterator(i); }

    public List subList(int a, int b)
    { return new IdList(id, inner.subList(a, b)); }

}
