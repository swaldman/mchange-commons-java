/*
 * Distributed as part of mchange-commons-java v.0.2.3.3
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
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


package com.mchange.v2.async;

import java.util.List;
import java.util.LinkedList;

/**
 * @deprecated CarefulRunnableQueue is better.
 */
public class SimpleRunnableQueue implements RunnableQueue, Queuable
{
    private List   taskList = new LinkedList();
    private Thread t        = new TaskThread();

    boolean gentle_close_requested = false;

    public SimpleRunnableQueue(boolean daemon)
    {
	t.setDaemon(daemon);
	t.start();
    }

    public SimpleRunnableQueue()
    { this( true ); }

    public RunnableQueue asRunnableQueue()
    { return this; }

    public synchronized void postRunnable(Runnable r)
    {
	if (gentle_close_requested)
	    throw new IllegalStateException("Attempted to post a task to a closed " +
					    "AsynchronousRunner.");

	taskList.add(r);
	this.notifyAll();
    }

    public synchronized void close( boolean skip_remaining_tasks )
    {
	if (skip_remaining_tasks)
	    t.interrupt();
	else
	    gentle_close_requested = true;
    }

    public synchronized void close()
    { this.close( true ); }

    private synchronized Runnable dequeueRunnable()
    {
	Runnable r = (Runnable) taskList.get(0);
	taskList.remove(0);
	return r;
    }

    private synchronized void awaitTask() throws InterruptedException
    {
	while (taskList.size() == 0) 
	    {
		if ( gentle_close_requested )
		    t.interrupt(); // remember t == Thread.currentThread()
		this.wait();
	    }
    }

    class TaskThread extends Thread
    {
	TaskThread()
	{ super("SimpleRunnableQueue.TaskThread"); }

	public void run()
	{
	    try
		{
		    while (! this.isInterrupted() )
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
		    if (Debug.DEBUG && Debug.TRACE >= Debug.TRACE_MED ) 
			System.err.println( this.toString() + " interrupted. Shutting down." );
		}
	    finally
		{
		    taskList = null;
		    t = null;
		}
	}
    }
}

