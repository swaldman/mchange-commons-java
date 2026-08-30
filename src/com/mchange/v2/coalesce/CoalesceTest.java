package com.mchange.v2.coalesce;

public class CoalesceTest
{
    final static int NUM_ITERS = 10000;

    final static Coalescer c = CoalescerFactory.createCoalescer( null, true, true );

    public static void main( String[] argv )
    {
	doTest();
	System.gc();
	System.err.println("num coalesced after gc: " + c.countCoalesced());
    }

    private static void doTest()
    {
	String[] strings = new String[ NUM_ITERS ];
	for (int i = 0; i < NUM_ITERS; ++i)
	    strings[i] = new String( "Hello" );

	long start_time = System.currentTimeMillis();
	for (int i = 0; i < NUM_ITERS; ++i)
	    {
		Object random = strings[i];
		Object normal = c.coalesce( random );
//     		System.err.println( System.identityHashCode( random ) +
//     				    "\t" +
//     				    System.identityHashCode( normal ) );
	    }
	long time_ms = System.currentTimeMillis() - start_time;
	System.out.println("avg time: " + time_ms / ((float) NUM_ITERS) +
			   "ms (" + NUM_ITERS + " iterations)");
	System.err.println("num coalesced: " + c.countCoalesced());
    }
}
