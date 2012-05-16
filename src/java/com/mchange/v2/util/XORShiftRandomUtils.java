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

package com.mchange.v2.util;

/*
 * A not-cryptographically-strong but decent and very fast
 * pseudorandom number generator. Algorithm can be inlined easily
 * for speed.
 *
 * Algorithm taken from...
 *
 *   http://javamex.com/tutorials/random_numbers/xorshift.shtml
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