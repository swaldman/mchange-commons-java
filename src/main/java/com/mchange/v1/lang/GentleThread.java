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
