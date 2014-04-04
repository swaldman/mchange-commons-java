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
