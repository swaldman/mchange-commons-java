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


package com.mchange.lang;

/**
 * @deprecated use com.mchange.v1.util.ArrayUtils
 * @author swaldman
 *
 */
public final class ArrayUtils
{
    /**
     *  The array may contain nulls, but <TT>o</TT>
     *  must be non-null.
     */
    public static int indexOf(Object[] array, Object o)
    {
	for (int i = 0, len = array.length; i < len; ++i)
	    if (o.equals(array[i])) return i;
	return -1;
    }
    
    public static int identityIndexOf(Object[] array, Object o)
    {
	for (int i = 0, len = array.length; i < len; ++i)
	    if (o == array[i]) return i;
	return -1;
    }

    /**
     * finds a hash value which takes into account
     * the value of all elements, such that two object
     * arrays for which Arrays.equals(a1, a2) returns
     * true will hashAll() to the same value
     */
    public static int hashAll(Object[] array)
    {
	int out = 0;
	for (int i = 0, len = array.length; i < len; ++i)
	    {
		Object o = array[i];
		if (o != null) out ^= o.hashCode();
	    }
	return out;
    }
    
    public static int hashAll(int[] array)
    {
	int out = 0;
	for (int i = 0, len = array.length; i < len; ++i)
	    out ^= array[i];
	return out;
    }

    public static boolean startsWith( byte[] checkMe, byte[] maybePrefix )
    {
	int cm_len = checkMe.length;
	int mp_len = maybePrefix.length;
	if (cm_len < mp_len)
	    return false;
	for (int i = 0; i < mp_len; ++i)
	    if (checkMe[i] != maybePrefix[i])
		return false;
	return true;
    }
    
    private ArrayUtils()
    {}
 }
