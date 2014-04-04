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

package com.mchange.v2.async.junit;

import junit.framework.*;
import com.mchange.v2.async.*;

public class ThreadPoolAsynchronousRunnerJUnitTestCase extends TestCase
{
    ThreadPoolAsynchronousRunner runner;

    boolean no_go = true;
    int gone = 0;

    protected void setUp() 
    {
	runner = new ThreadPoolAsynchronousRunner( 3,
						   true,
						   1000,
						   3 * 1000,
						   3 * 1000);
    }

    protected void tearDown() 
    { 
	runner.close(); 
	go(); //get any interrupt ignorers going...
    }

    private synchronized void go()
    {
	no_go = false;
	this.notifyAll();
    }

    public void testDeadlockCase()
    {
	try
	    {
		DumbTask dt = new DumbTask( true );
		for( int i = 0; i < 5; ++i )
		    runner.postRunnable( dt );
		Thread.sleep(500);
		assertEquals("we should have three running tasks", 3, runner.getActiveCount() );
		assertEquals("we should have two pending tasks", 2, runner.getPendingTaskCount() );
		Thread.sleep(10000); // not strictly safe, but should be plenty of time to interrupt and be done
	    }
	catch (InterruptedException e)
	    {
		e.printStackTrace();
		fail("Unexpected InterruptedException: " + e);
	    }
    }

    class DumbTask implements Runnable
    {
	boolean ignore_interrupts;

	DumbTask()
	{ this( false ); }

	DumbTask(boolean ignore_interrupts)
	{ this.ignore_interrupts = ignore_interrupts; }

	public void run()
	{
	    try
		{
		    synchronized (ThreadPoolAsynchronousRunnerJUnitTestCase.this)
			{
			    while (no_go)
				{
				    try { ThreadPoolAsynchronousRunnerJUnitTestCase.this.wait(); }
				    catch (InterruptedException e)
					{
					    if (ignore_interrupts)
						System.err.println(this + ": interrupt ignored!");
					    else
						{
						    e.fillInStackTrace();
						    throw e;
						}
					}
				}
			    //System.err.println( ++gone );
			    ThreadPoolAsynchronousRunnerJUnitTestCase.this.notifyAll();
			}
		}
	    catch ( Exception e )
		{ e.printStackTrace(); }
	}
    }

    public static void main(String[] argv)
    { 
	junit.textui.TestRunner.run( new TestSuite( ThreadPoolAsynchronousRunnerJUnitTestCase.class ) ); 
	//junit.swingui.TestRunner.run( SqlUtilsJUnitTestCase.class ); 
	//new SqlUtilsJUnitTestCase().testGoodDebugLoggingOfNestedExceptions();
    }
}
