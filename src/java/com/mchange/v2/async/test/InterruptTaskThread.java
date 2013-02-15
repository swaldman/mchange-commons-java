/*
 * Distributed as part of mchange-commons-java v.0.2.4
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


package com.mchange.v2.async.test;

import java.util.*;
import com.mchange.v2.async.*;
import com.mchange.v2.lang.ThreadUtils;

public class InterruptTaskThread
{
    static Set interruptedThreads = Collections.synchronizedSet( new HashSet() );

    public static void main( String[] argv )
    {
	try
	    {
		AsynchronousRunner runner = new RoundRobinAsynchronousRunner( 5, false );
		new Interrupter().start();
		for (int i = 0; i < 1000; ++i)
		    {
			try
			    { runner.postRunnable( new DumbTask() ); }
			catch ( Exception e )
			    { e.printStackTrace(); }
			Thread.sleep(50);
		    }
		System.out.println("Interrupted Threads: " + interruptedThreads.size());
	    }
	catch ( Exception e )
	    { e.printStackTrace(); }
    }

    static class Interrupter extends Thread
    {
	public void run()
	{
	    try
		{
		    while (true)
			{
			    Thread[] fillMe = new Thread[1000];
			    ThreadUtils.enumerateAll( fillMe );
			    for(int i = 0; fillMe[i] != null; ++i)
				{
				    if (fillMe[i].getName().indexOf("RunnableQueue.TaskThread") >= 0)
					{
					    fillMe[i].interrupt();
					    System.out.println("INTERRUPTED!");
					    interruptedThreads.add( fillMe[i] );
					    break;
					}
				}
			    Thread.sleep(1000);
			}
		}
	    catch ( Exception e )
		{ e.printStackTrace(); }
	}
    }

    static class DumbTask implements Runnable
    {
	static int count = 0;

	static synchronized int number() { return count++; }

	public void run()
	{
	    try
		{
		    Thread.sleep(200);
		    System.out.println("DumbTask complete! " + number());
		}
	    catch ( Exception e )
		{ e.printStackTrace(); }
	}
    }
}
