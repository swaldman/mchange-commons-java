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

package com.mchange.v1.util;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


/**
 * @deprecated use com.mchange.v1.resourcepool.AbstractResourcePool
 */
public abstract class AbstractResourcePool
{
    private final static boolean TRACE = true;
    private final static boolean DEBUG = true;

    private static RunnableQueue sharedQueue = new SimpleRunnableQueue();

    Set  managed = new HashSet();
    List unused  = new LinkedList();

    int start;
    int max;
    int inc;

    int num_acq_attempts  = Integer.MAX_VALUE;
    int acq_attempt_delay = 50;

    RunnableQueue rq;
    
    boolean initted = false;
    boolean broken  = false;
    

    protected AbstractResourcePool(int start, int max, int inc)
    {this(start, max, inc, sharedQueue);}

    protected AbstractResourcePool(int start, int max, int inc, RunnableQueue rq)
    {
	this.start = start;
	this.max   = max;
	this.inc   = inc;
	this.rq    = rq;
    }

    protected abstract Object acquireResource() throws Exception;

    /** Called on checkout! */
    protected abstract void   refurbishResource(Object resc) throws BrokenObjectException;

    protected abstract void   destroyResource(Object resc) throws Exception;

    /**
     * We defer actual acquisition of the resources to a 
     * method outside the constructor because subclasses
     * may need to do prep work in their own constructor
     * before resource acquisition can occur. This method
     * will usually be called at the end of a subclasses
     * constructor.
     */
    protected synchronized void init() throws Exception
    {
	for (int i = 0; i < start; ++i) assimilateResource();

	this.initted = true;
    }

    //we synchronize in the delegate method
    protected Object checkoutResource() 
	throws BrokenObjectException, InterruptedException, Exception
    { return checkoutResource( 0 ); }

    /* Note that if an any exception occurs on refurbishResource(), */
    /* we remove and retry our checkout                             */
    protected synchronized Object checkoutResource(long timeout) 
	throws BrokenObjectException, InterruptedException, TimeoutException, Exception
    {
	if (!initted) init();
	ensureNotBroken();

	int sz = unused.size();
	 //System.err.println("pool size: " + sz);
	if (sz == 0)
	    {
		int msz = managed.size();
		if (msz < max)
		    postAcquireMore();
		awaitAvailable(timeout);
	    }
	Object resc = unused.get(0);
	unused.remove(0);
	try
	    {
		refurbishResource(resc);
	    }
	catch (Exception e)
	    {
		//uh oh... bad resource...
		if (DEBUG) e.printStackTrace();
		removeResource(resc);
		return checkoutResource(timeout);
	    }
	if (TRACE) trace();
	return resc;
    }

    protected synchronized void checkinResource(Object resc) throws BrokenObjectException
    {
	//we permit straggling resources to be checked in 
	//without exception even if we are broken
	if (!managed.contains(resc))
	    throw new IllegalArgumentException("ResourcePool: Tried to check-in a foreign resource!");
	unused.add(resc);
	this.notifyAll();
	if (TRACE) trace();
    }

    protected synchronized void markBad(Object resc) throws Exception
    { removeResource( resc ); }

    protected synchronized void close() throws Exception
    {
				//we permit closes when we are already broken, so
				//that resources that were checked out when the break
				//occured can still be cleaned up
	this.broken = true;
	for (Iterator ii = managed.iterator(); ii.hasNext();)
	    {
		try
		    {removeResource(ii.next());}
		catch (Exception e)
		    {if (DEBUG) e.printStackTrace();}
	    }
    }



    //the following methods should only be invoked from 
    //sync'ed methods / blocks...

    private void postAcquireMore() throws InterruptedException
    {
	rq.postRunnable(new AcquireTask());
    }

    private void awaitAvailable(long timeout) throws InterruptedException, TimeoutException
    {
	int avail;
	while ((avail = unused.size()) == 0) this.wait(timeout);
	if (avail == 0)
	    throw new TimeoutException();
    }

    private void acquireMore() throws Exception
    {
	int msz = managed.size();
	for (int i = 0; i < Math.min(inc, max - msz); ++i)
	    assimilateResource();
    }

    private void assimilateResource() throws Exception
    {
	Object resc = acquireResource();
	managed.add(resc);
	unused.add(resc);
	//System.err.println("assimilate resource... unused: " + unused.size());
	this.notifyAll();
	if (TRACE) trace();
    }

    private void removeResource(Object resc) throws Exception
    {
	managed.remove(resc);
	unused.remove(resc);
	destroyResource(resc);
	if (TRACE) trace();
    }

    private void ensureNotBroken() throws BrokenObjectException
    {if (broken) throw new BrokenObjectException(this);}

    //same as close, but we do not destroy checked out
    //resources
    private synchronized void unexpectedBreak()
    {
	this.broken = true;
	for (Iterator ii = unused.iterator(); ii.hasNext();)
	    {
		try
		    {removeResource(ii.next());}
		catch (Exception e)
		    {if (DEBUG) e.printStackTrace();}
	    }
    }

    private void trace()
    {
	System.err.println(this + "  [managed: " + managed.size() + ", " +
			   "unused: " + unused.size() + ']');
    }

    class AcquireTask implements Runnable
    {
	boolean success = false;
	
	public void run()
	{
	    for (int i = 0; !success && i < num_acq_attempts; ++i)
	    {
		try
		    {
			if (i > 0)
			    Thread.sleep(acq_attempt_delay); 
			synchronized (AbstractResourcePool.this)
			    { acquireMore(); }
			success = true;
		    }
		catch (Exception e)
		    {if (DEBUG) e.printStackTrace();}
	    }
	    if (!success) unexpectedBreak();
	}
    }

    protected class TimeoutException extends Exception
    {}
}








