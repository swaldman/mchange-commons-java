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

