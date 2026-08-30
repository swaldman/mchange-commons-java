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
