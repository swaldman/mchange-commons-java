/*
 * Distributed as part of mchange-commons-java v.0.2.3.2
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

