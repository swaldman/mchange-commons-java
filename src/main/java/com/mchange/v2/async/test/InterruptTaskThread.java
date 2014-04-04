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
