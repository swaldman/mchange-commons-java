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

import java.lang.ref.*;
import java.util.*;
import com.mchange.v1.util.WrapperIterator;

/**
 * IdWeakHashMap is NOT null-accepting!
 */
public final class IdWeakHashMap extends IdMap implements Map
{
    ReferenceQueue rq;

    public IdWeakHashMap(Identicator id)
    { 
	super ( new HashMap(), id ); 
	this.rq = new ReferenceQueue();
    }

    //all methods from Map interface
    public int size()
    {
	// doing cleanCleared() afterwards, as with other methods
	// would be just as "correct", as weak collections
	// make no guarantees about when things disappear,
	// but for size(), it feels a little more accurate
	// this way.
	cleanCleared();
	return super.size();
    }

    public boolean isEmpty()
    {
	try
	    { return super.isEmpty(); }
	finally
	    { cleanCleared(); }
    }

    public boolean containsKey(Object o)
    {
	try
	    { return super.containsKey( o ); }
	finally
	    { cleanCleared(); }
    }

    public boolean containsValue(Object o)
    {
	try
	    { return super.containsValue( o ); }
	finally
	    { cleanCleared(); }
    }

    public Object get(Object o)
    {
	try
	    { return super.get( o ); }
	finally
	    { cleanCleared(); }
    }

    public Object put(Object k, Object v)
    {
	try
	    { return super.put( k , v ); }
	finally
	    { cleanCleared(); }
    }

    public Object remove(Object o)
    {
	try
	    { return super.remove( o ); }
	finally
	    { cleanCleared(); }
    }

    public void putAll(Map m)
    {
	try
	    { super.putAll( m ); }
	finally
	    { cleanCleared(); }
    }

    public void clear()
    {
	try
	    { super.clear(); }
	finally
	    { cleanCleared(); }
    }

    public Set keySet()
    {
	try
	    { return super.keySet(); }
	finally
	    { cleanCleared(); }
    }

    public Collection values()
    {
	try
	    { return super.values(); }
	finally
	    { cleanCleared(); }
    }

    /*
     * entrySet() is the basis of the implementation of the other
     * Collection returning methods. Get this right and the rest 
     * follow.
     */
    public Set entrySet()
    {
	try
	    { return new WeakUserEntrySet(); }
	finally
	    { cleanCleared(); }
    }

    public boolean equals(Object o)
    {
	try
	    { return super.equals( o ); }
	finally
	    { cleanCleared(); }
    }

    public int hashCode()
    {
	try
	    { return super.hashCode(); }
	finally
	    { cleanCleared(); }
    }

    //internal methods
    protected IdHashKey createIdKey(Object o)
    { return new WeakIdHashKey( o, id, rq ); }

    private void cleanCleared()
    {
	WeakIdHashKey.Ref ref;
	while ((ref = (WeakIdHashKey.Ref) rq.poll()) != null)
	    this.removeIdHashKey( ref.getKey() );
    }

    private final class WeakUserEntrySet extends AbstractSet
    {
	Set innerEntries = internalEntrySet();
	
	public Iterator iterator()
	{
	    try
		{
		    return new WrapperIterator(innerEntries.iterator(), true)
			{
			    protected Object transformObject(Object o)
			    {
				Entry innerEntry = (Entry) o;
				final Object userKey = ((IdHashKey) innerEntry.getKey()).getKeyObj();
				if (userKey == null)
				    return WrapperIterator.SKIP_TOKEN;
				else
				    return new UserEntry( innerEntry ) 
					{ Object preventRefClear = userKey; };
			    }
			};
		}
	finally
	    { cleanCleared(); }
	}
	
	public int size()
	{ 
	    // doing cleanCleared() afterwards, as with other methods
	    // would be just as "correct", as weak collections
	    // make no guarantees about when things disappear,
	    // but for size(), it feels a little more accurate
	    // this way.
	    cleanCleared();
	    return innerEntries.size(); 
	}
	
	public boolean contains(Object o)
	{ 
	    try
		{
		    if (o instanceof Entry)
			{
			    Entry entry = (Entry) o;
			    return innerEntries.contains( createIdEntry( entry ) ); 
			}
		    else
			return false;
		}
	    finally
		{ cleanCleared(); }
	}
	
	public boolean remove(Object o)
	{
	    try
		{
		    if (o instanceof Entry)
			{
			    Entry entry = (Entry) o;
			    return innerEntries.remove( createIdEntry( entry ) ); 
			}
		    else
			return false;
		}
	    finally
		{ cleanCleared(); }
	}

	public void clear()
	{
	    try
		{ inner.clear(); }
	    finally
		{ cleanCleared(); }
	}
    }
}
