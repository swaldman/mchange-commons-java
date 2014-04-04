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

package com.mchange.v2.util;

/**
 *  A not-cryptographically-strong but decent and very fast
 *  pseudorandom number generator. Algorithm can be inlined easily
 *  for speed.
 *
 *  Algorithm taken from...
 *
 *    http://javamex.com/tutorials/random_numbers/xorshift.shtml
 *
 */
public final class XORShiftRandomUtils
{
    public static long nextLong(long prev)
    {
	prev ^= (prev << 21);
	prev ^= (prev >>> 35);
	prev ^= (prev << 4);
	return prev;
    }

    public static void main(String[] argv)
    {
	long x = System.currentTimeMillis();
	int len = 100;
	int[] counts = new int[len];

	for(int i = 0; i < 1000000; ++i)
	    {
		x = nextLong(x);
		++counts[ (int)(Math.abs(x) % len) ];
		if ((i % 10000) == 0)
		    System.out.println( x );
	    }
	for (int i = 0; i < len; ++i)
	    {
		if (i != 0) System.out.print(", ");
		System.out.print( i + " -> " + counts[i]);
	    }
	System.out.println();
    }
}
