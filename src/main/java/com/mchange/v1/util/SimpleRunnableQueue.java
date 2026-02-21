package com.mchange.v1.util;

import java.util.List;
import java.util.LinkedList;

/**
 * @deprecated use com.mchange.v2.async.SimpleRunnableQueue
 */
public class SimpleRunnableQueue implements RunnableQueue
{
    private List   taskList = new LinkedList();
    private Thread t        = new TaskThread();

    public SimpleRunnableQueue(boolean daemon)
    {
	t.setDaemon(daemon);
	t.start();
    }

    public SimpleRunnableQueue()
    { this( true ); }

    public synchronized void postRunnable(Runnable r)
    {
	taskList.add(r);
	this.notifyAll();
    }

    public synchronized void close()
    {
	t.interrupt();
	taskList = null;
	t = null;
    }

    private synchronized Runnable dequeueRunnable()
    {
	Runnable r = (Runnable) taskList.get(0);
	taskList.remove(0);
	return r;
    }

    private synchronized void awaitTask() throws InterruptedException
    {while (taskList.size() == 0) this.wait();}

    class TaskThread extends Thread
    {
	TaskThread()
	{ super("SimpleRunnableQueue.TaskThread"); }

	public void run()
	{
	    try
		{
		    while (true)
			{
			    awaitTask();
			    Runnable r = dequeueRunnable();
			    try
				{ r.run(); }
			    catch (Exception e)
				{
				    System.err.println(this.getClass().getName() +
						       " -- Unexpected exception in task!");
				    e.printStackTrace();
				}
			}
		}
	    catch (InterruptedException e)
		{
		    if (Debug.DEBUG) 
			System.err.println( this.toString() + " interrupted. Shutting down." );
		}
	}
    }
}

