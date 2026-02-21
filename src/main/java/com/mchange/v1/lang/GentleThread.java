package com.mchange.v1.lang;

/**
 * an abstract Thread class that provides
 * utilities for easily defining Threads with
 * safe versions of the deprecated thread
 * methods stop(), resume(), and start()
 */
public abstract class GentleThread extends Thread
{
    boolean should_stop = false;
    boolean should_suspend = false;
    
    public GentleThread()
    { super(); }

    public GentleThread(String name)
    { super( name ); }

    public abstract void run();

    /**
     * a safe method for stopping properly implemented GentleThreads
     */
    public synchronized void gentleStop()
    {should_stop = true;}

    /**
     * a safe method for suspending properly implemented GentleThreads
     */
    public synchronized void gentleSuspend()
    {should_suspend = true;}

    /**
     * a safe method for resuming properly implemented GentleThreads
     */
    public synchronized void gentleResume()
    {
	should_suspend = false;
	this.notifyAll();
    }

    /**
     * tests whether the thread should stop.
     * Subclasses should call this method periodically in 
     * their run method, and return from run() is the
     * method returns true.
     */
    protected synchronized boolean shouldStop()
    {return should_stop;}

    /**
     * tests whether the thread should suspend.
     * Subclasses rarely call this method directly,
     * and should call allowSuspend() periodically
     * instead.
     *
     * @see #allowSuspend
     */
    protected synchronized boolean shouldSuspend()
    {return should_suspend;}

    /**
     * tests whether the thread should suspend,
     * and causes to the thread to pause if appropriate.
     * Subclasses should call this method periodically
     * in their run method to, um, allow suspension.
     * Threads paused by allowSuspend() will be properly
     * awoken by gentleResume()
     *
     * @see #gentleResume
     */
    protected synchronized void allowSuspend() throws InterruptedException 
    {while (should_suspend) this.wait();}
}
