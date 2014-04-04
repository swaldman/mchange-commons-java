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

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;
import com.mchange.v2.util.ResourceClosedException;

public class CarefulRunnableQueue implements RunnableQueue, Queuable, StrandedTaskReporting
{
    private final static MLogger logger = MLog.getLogger( CarefulRunnableQueue.class );

    private List taskList = new LinkedList();
    private TaskThread t  = new TaskThread();

    private boolean shutdown_on_interrupt;

    private boolean gentle_close_requested = false;

    private List strandedTasks = null;

    public CarefulRunnableQueue(boolean daemon, boolean shutdown_on_interrupt)
    {
	this.shutdown_on_interrupt = shutdown_on_interrupt;
	t.setDaemon( daemon );
	t.start();
    }

    public RunnableQueue asRunnableQueue()
    { return this; }

    public synchronized void postRunnable(Runnable r)
    {
	try
	    {
		if (gentle_close_requested)
		    throw new ResourceClosedException("Attempted to post a task to a closing " +
						      "CarefulRunnableQueue.");
		
		taskList.add(r);
		this.notifyAll();
	    }
	catch (NullPointerException e)
	    {
		//e.printStackTrace();
		if (Debug.DEBUG)
		    {
			if ( logger.isLoggable( MLevel.FINE ) )
			    logger.log( MLevel.FINE, "NullPointerException while posting Runnable.", e );
		    }
		if (taskList == null)
		    throw new ResourceClosedException("Attempted to post a task to a CarefulRunnableQueue " +
						      "which has been closed, or whose TaskThread has been " +
						      "interrupted.");
		else 
		    throw e;
	    }
    }

    public synchronized void close( boolean skip_remaining_tasks )
    {
	if (skip_remaining_tasks)
	    {
		t.safeStop();
		t.interrupt();
	    }
	else
	    gentle_close_requested = true;
    }

    public synchronized void close()
    { this.close( true ); }

    public synchronized List getStrandedTasks()
    {
	try
	    {
		while (gentle_close_requested && taskList != null)
		    this.wait();
		return strandedTasks;
	    }
	catch (InterruptedException e) 
	    {
		// very, very rare I think...
		// if necessary I'll try a more complex solution, but I don't think
		// it's worth it.
		//e.printStackTrace();
		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.log( MLevel.WARNING, 
				Thread.currentThread() + " interrupted while waiting for stranded tasks from CarefulRunnableQueue.",
				e );

		throw new RuntimeException(Thread.currentThread() + 
					   " interrupted while waiting for stranded tasks from CarefulRunnableQueue.");
	    }
    }

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
		    {
			t.safeStop(); // remember t == Thread.currentThread()
			t.interrupt();
		    }
		this.wait();
	    }
    }

    class TaskThread extends Thread
    {
	boolean should_stop = false;

	TaskThread()
	{ super("CarefulRunnableQueue.TaskThread"); }

	public synchronized void safeStop()
	{ should_stop = true; }

	private synchronized boolean shouldStop()
	{ return should_stop; }

	public void run()
	{
	    try
		{
		    while (! shouldStop() )
			{
			    try
				{
				    awaitTask();
				    Runnable r = dequeueRunnable();
				    try
					{ r.run(); }
				    catch (Exception e)
					{
					    //System.err.println(this.getClass().getName() + " -- Unexpected exception in task!");
					    //e.printStackTrace();

					    if ( logger.isLoggable( MLevel.WARNING ) )
						logger.log(MLevel.WARNING, this.getClass().getName() + " -- Unexpected exception in task!", e);
					}
				}
			    catch (InterruptedException e)
				{
				    if (shutdown_on_interrupt)
					{
					    CarefulRunnableQueue.this.close( false );
// 					    if (Debug.DEBUG && Debug.TRACE >= Debug.TRACE_MED ) 
// 						System.err.println( this.toString() + 
// 								    " interrupted. Shutting down after current tasks" +
// 								    " have completed." );
					    if ( logger.isLoggable( MLevel.INFO ) )
						logger.info(this.toString() + 
							    " interrupted. Shutting down after current tasks" +
							    " have completed." );
					}
				    else
					{
// 					    if (Debug.DEBUG && Debug.TRACE >= Debug.TRACE_MED ) 
// 						System.err.println( this.toString() + 
// 								    " received interrupt. IGNORING." );
					    logger.info(this.toString() + " received interrupt. IGNORING." );
					}
				}
			}
		}
// 	    catch (ThreadDeath td) //DEBUG ONLY -- remove soon, swaldman 08-Jun-2003
// 		{
// 		    System.err.print("c3p0-TRAVIS: ");
// 		    System.err.println(this.getName() + ": Some bastard used the deprecated stop() method to kill me!!!!");
// 		    td.printStackTrace();
// 		    throw td;
// 		}
// 	    catch (Throwable t) //DEBUG ONLY -- remove soon, swaldman 08-Jun-2003
// 		{
// 		    System.err.print("c3p0-TRAVIS: ");
// 		    System.err.println(this.getName() + ": Some unexpected Throwable occurred and killed me!!!!");
// 		    t.printStackTrace();
// 		    if (t instanceof Error)
// 			throw (Error) t;
// 		    else if (t instanceof RuntimeException)
// 			throw (RuntimeException) t;
// 		    else
// 			throw new InternalError( t.toString() ); //we don't expect any checked Exceptions can happen here.
// 		}
	    finally
		{
		    synchronized ( CarefulRunnableQueue.this )
			{
			    strandedTasks = Collections.unmodifiableList( taskList );
			    taskList = null;
			    t = null;
			    CarefulRunnableQueue.this.notifyAll(); //if anyone is waiting for stranded tasks...
			    //System.err.print("c3p0-TRAVIS: ");
			    //System.err.println("TaskThread dead. strandedTasks: " + strandedTasks);
			}
		}
	}
    }
}

