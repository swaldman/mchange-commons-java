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

import com.mchange.v2.log.*;
import com.mchange.v2.util.ResourceClosedException;

/**
 * A class that provides for effecient asynchronous execution
 * of multiple tasks that may block, but that do not contend
 * for the same locks. The order in which tasks will be executed
 * is not guaranteed.
 */
public class RoundRobinAsynchronousRunner implements AsynchronousRunner, Queuable
{
    private final static MLogger logger = MLog.getLogger( RoundRobinAsynchronousRunner.class );

    //MT: unchanging, individual elements are thread-safe
    final RunnableQueue[] rqs;

    //MT: protected by this' lock
    int task_turn = 0;

    //MT: protected by this' lock
    int view_turn = 0;

    public RoundRobinAsynchronousRunner( int num_threads, boolean daemon )
    {
	this.rqs = new RunnableQueue[ num_threads ];
	for(int i = 0; i < num_threads; ++i)
	    rqs[i] = new CarefulRunnableQueue( daemon, false );
    }

    public synchronized void postRunnable(Runnable r)
    { 
	try
	    {
		int index = task_turn;
		task_turn = (task_turn + 1) % rqs.length;
		rqs[index].postRunnable( r );

		/* we do this "long-hand" to avoid bad fragility if an exception */
		/* occurs in postRunnable, causing the mod step of the original  */
		/* concise code to get skipped, and leading (if                  */
		/* task_turn == rqs.length - 1 when the exception occurs) to an  */
		/* endless cascade of ArrayIndexOutOfBoundsExceptions.           */
		/* we might alternatively have just put the mod step into a      */
		/* finally block, but that's too fancy.                          */
		/* thanks to Travis Reeder for reporting this problem.           */

		//rqs[task_turn++].postRunnable( r );
		//task_turn %= rqs.length;
	    }
	catch ( NullPointerException e )
	    {
		//e.printStackTrace();
		if ( Debug.DEBUG )
		    {
			if ( logger.isLoggable( MLevel.FINE ) )
			    logger.log( MLevel.FINE, "NullPointerException while posting Runnable -- Probably we're closed.", e );
		    }
		this.close( true );
		throw new ResourceClosedException("Attempted to use a RoundRobinAsynchronousRunner in a closed or broken state.");
	    }
    }

    public synchronized RunnableQueue asRunnableQueue()
    { 
	try
	    {
		int index = view_turn;
		view_turn = (view_turn + 1) % rqs.length;
		return new RunnableQueueView( index );
		
		/* same explanation as above */
		
		//RunnableQueue out = new RunnableQueueView( view_turn++ ); 
		//view_turn %= rqs.length;
		//return out;
	    }
	catch ( NullPointerException e )
	    {
		//e.printStackTrace();
		if ( Debug.DEBUG )
		    {
			if ( logger.isLoggable( MLevel.FINE ) )
			    logger.log( MLevel.FINE, "NullPointerException in asRunnableQueue() -- Probably we're closed.", e );
		    }
		this.close( true );
		throw new ResourceClosedException("Attempted to use a RoundRobinAsynchronousRunner in a closed or broken state.");
	    }
    }

    public synchronized void close( boolean skip_remaining_tasks )
    {
	for (int i = 0, len = rqs.length; i < len; ++i)
	    {
		attemptClose( rqs[i], skip_remaining_tasks );
		rqs[i] = null;
	    }
    }

    public void close()
    { close( true ); }

    static void attemptClose(RunnableQueue rq, boolean skip_remaining_tasks)
    {
	try { rq.close( skip_remaining_tasks ); }
	catch ( Exception e ) 
	    { 
		//e.printStackTrace(); 
		if ( logger.isLoggable( MLevel.WARNING ) )
		    logger.log( MLevel.WARNING, "RunnableQueue close FAILED.", e );
	    }
    }

    class RunnableQueueView implements RunnableQueue
    {
	final int rq_num;

	RunnableQueueView( int rq_num )
	{ this.rq_num = rq_num; }

	public void postRunnable(Runnable r)
	{ rqs[ rq_num ].postRunnable( r ); }
	
	public void close( boolean skip_remaining_tasks )
	{ }
	
	public void close()
	{ /* ignore */ }
    }
}
