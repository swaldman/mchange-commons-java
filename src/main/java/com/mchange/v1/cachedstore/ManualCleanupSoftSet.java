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

package com.mchange.v1.cachedstore;

import java.util.*;
import java.lang.ref.*;
import com.mchange.v1.util.WrapperIterator;

class ManualCleanupSoftSet extends AbstractSet implements Vacuumable
{
    HashSet inner = new HashSet();
    ReferenceQueue queue = new ReferenceQueue();

    public Iterator iterator()
    {
	return new WrapperIterator( inner.iterator(), true )
	    {
		protected Object transformObject(Object o)
		{
		    SoftKey sk = (SoftKey) o;
		    Object out = sk.get();
		    return (out == null ? SKIP_TOKEN : out);
		}
	    };
    }

    /** 
     * this size may not be completely accurate, because
     * keys in the set may have cleared. In general with
     * this call, one must presume that elements may at
     * unpredictable times, simply "disappear".
     */
    public int size()
    { return inner.size(); }

    public boolean contains(Object o)
    { return inner.contains( new SoftKey( o, null ) ); }

    private ArrayList toArrayList()
    {
	ArrayList out = new ArrayList( this.size() );
	for (Iterator ii = this.iterator(); ii.hasNext();)
	    out.add( ii.next() );
	return out;
    }

    public Object[] toArray() 
    { return this.toArrayList().toArray(); }

    public Object[] toArray(Object[] a) 
    { return this.toArrayList().toArray(a); }

    public boolean add(Object o) 
    { return inner.add( new SoftKey(o, queue) ); }

    public boolean remove(Object o) 
    { return inner.remove( new SoftKey( o, null ) ); }

    public void clear()
    { inner.clear(); }

    public void vacuum() throws CachedStoreException
    { 
	SoftKey key;
	while ((key = (SoftKey) queue.poll()) != null)
	    inner.remove( key );
    }
}
