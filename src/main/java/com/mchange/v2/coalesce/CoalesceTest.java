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
