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
import java.lang.reflect.Array;

public final class IteratorUtils
{
    public final static Iterator EMPTY_ITERATOR = new Iterator()
    {
	public boolean hasNext()
	{ return false; }

	public Object next()
	{ throw new NoSuchElementException(); }

	public void remove()
	{ throw new IllegalStateException(); }
    };

    public static Iterator oneElementUnmodifiableIterator(final Object elem)
    {
	return new Iterator()
	    {
		boolean shot = false;

		public boolean hasNext() { return (!shot); }

		public Object next()
		{
		    if (shot)
			throw new NoSuchElementException();
		    else
			{
			    shot = true;
			    return elem;
			}
		}

		public void remove()
		{ throw new UnsupportedOperationException("remove() not supported."); }
	    };
    }

    public static boolean equivalent(Iterator ii, Iterator jj)
    {
	while (true)
	    {
		boolean ii_hasnext = ii.hasNext();
		boolean jj_hasnext = jj.hasNext();
		if (ii_hasnext ^ jj_hasnext)
		    return false;
		else if (ii_hasnext)
		    {
			Object iiNext = ii.next();
			Object jjNext = jj.next();
			if (iiNext == jjNext)
			    continue;
			else if (iiNext == null)
			    return false;
			else if (!iiNext.equals(jjNext))
			    return false;
		    }
		else return true;
	    }
    }

    public static ArrayList toArrayList(Iterator ii, int initial_capacity)
    {
	ArrayList out = new ArrayList(initial_capacity);
	while (ii.hasNext())
	    out.add(ii.next());
	return out;
    }

    /**
     * Fills an array with the contents of an iterator. If the array is too small,
     * it will contain the first portion of the iterator. If the array can contain
     * more elements than the iterator, extra elements are left untouched, unless
     * null_terminate is set to true, in which case the element immediately following
     * the last from the iterator is set to null. (This method is intended to make
     * it easy to implement Collection.toArray(Object[] oo) methods...
     *
     * @param null_terminate iff there is extra space in the array, set the element
     *        immediately after the last from the iterator to null.
     */
    public static void fillArray(Iterator ii, Object[] fillMe, boolean null_terminate)
    {
	int i = 0;
	int len = fillMe.length;
	while ( i < len && ii.hasNext() )
	    fillMe[ i++ ] = ii.next();
	if (null_terminate && i < len)
	    fillMe[i] = null;
    }

    public static void fillArray(Iterator ii, Object[] fillMe)
    { fillArray( ii, fillMe, false); }

    /**
     * @param null_terminate iff there is extra space in the array, set the element
     *        immediately after the last from the iterator to null.
     */
    public static Object[] toArray(Iterator ii, int array_size, Class componentClass, boolean null_terminate)
    {
	Object[] out = (Object[]) Array.newInstance( componentClass, array_size );
	fillArray(ii, out, null_terminate);
	return out;
    }

    public static Object[] toArray(Iterator ii, int array_size, Class componentClass)
    { return toArray( ii, array_size, componentClass, false ); }

    /**
     * Designed to help implement Collection.toArray(Object[] )methods... does
     * the right thing if you can express an iterator and know the size of your
     * Collection.
     */
    public static Object[] toArray(Iterator ii, int ii_size, Object[] maybeFillMe)
    {
	if (maybeFillMe.length >= ii_size)
	    {
		fillArray( ii, maybeFillMe, true );
		return maybeFillMe;
	    }
	else
	    {
		Class componentType = maybeFillMe.getClass().getComponentType(); 
		return toArray( ii, ii_size, componentType );
	    }
    }

    private IteratorUtils()
    {}
}


