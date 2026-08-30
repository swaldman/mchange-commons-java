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
