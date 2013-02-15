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

package com.mchange.v2.lock;

import java.util.*;

/**
 * <p>Fully reentrant. We could still separate the shared and exclusive wait() sets for greater efficiency
 * under circumstances of high contention, but it would require extra synchronizations even under low
 * contention, so we'll leave this as is, and maybe write a different class that presumes large wait
 * sets.</p>
 *
 * <p>Because this class is reentrant, lock relinquishes can't be idempotent -- we have to keep track of
 * precisely how many times the lock was acquired and then release. Clients therefore have to be very
 * careful that every acquisition is matched by exactly one relinquish. The way to do this is to acquire
 * locks immediately <i>before</i> a try lock, and putting the relinquish in the finally block. One
 * should not attempt to acquire the locks within the try block, because if the lock acquisition is
 * interrupted, the lock relinquish in the finally block will run without the lock acquisition having
 * succeeded.</p>
 */
public class ExactReentrantSharedUseExclusiveUseLock implements SharedUseExclusiveUseLock
{
    Set  waitingShared = new HashSet();    //can't reenter this set, 'cuz once a thread enters, it waits
    List activeShared  = new LinkedList(); //can reenter this, so we are duplicate-holding Collection

    Set waitingExclusive   = new HashSet();
    Thread activeExclusive = null;

    int exclusive_shared_reentries    = 0;
    int exclusive_exclusive_reentries = 0;

    String name;

    public ExactReentrantSharedUseExclusiveUseLock( String name )
    { this.name = name; }

    public ExactReentrantSharedUseExclusiveUseLock()
    { this(null); }

    void status( String afterMethod )
    {
	System.err.println( this + " -- after " + afterMethod );
	System.err.println( "waitingShared: " + waitingShared );
	System.err.println( "activeShared: "  + activeShared );
	System.err.println( "waitingExclusive: " + waitingExclusive );
	System.err.println( "activeExclusive: "  + activeExclusive );
	System.err.println( "exclusive_shared_reentries: " + exclusive_shared_reentries );
	System.err.println( "exclusive_exclusive_reentries: " + exclusive_exclusive_reentries );
	System.err.println( " ---- " );
	System.err.println( );
    }

    public synchronized void acquireShared() throws InterruptedException
    {
	Thread t = Thread.currentThread();
	if ( t == activeExclusive )
	    ++exclusive_shared_reentries;
	else
	    {
		try
		    {
			waitingShared.add( t );
			while (! okayForShared())
			    this.wait();
			activeShared.add( t );
		    }
		finally
		    { 
			waitingShared.remove( t );
		    }
	    }
	//status("acquireShared()");
    }

    public synchronized void relinquishShared()
    { 
	Thread t = Thread.currentThread();
	if ( t == activeExclusive )
	    {
		--exclusive_shared_reentries;
		if ( exclusive_shared_reentries < 0 )
		    throw new IllegalStateException( t + " relinquished a shared lock (reentrant on exclusive) it did not hold!" );
	    }
	else
	    {
		// note this covers the case where a thread has an exclusive lock
		// and some arbitrary thread tries to relinquish a lock it did not
		// hold.

		boolean check = activeShared.remove( t );
		if (! check)
		    throw new IllegalStateException( t + " relinquished a shared lock it did not hold!" );
		this.notifyAll();
	    }
	//status("relinquishShared()");
    }

    public synchronized void acquireExclusive() throws InterruptedException
    {
	Thread t = Thread.currentThread();
	if ( t == activeExclusive )
	    ++exclusive_exclusive_reentries;
	else
	    {
		try
		    {
			waitingExclusive.add ( t );
			while (! okayForExclusive( t ))
			    this.wait();
			activeExclusive = t;
		    }
		finally
		    {
			waitingExclusive.remove( t );
		    }
	    }
	//status("acquireExclusive()");
    }

    public synchronized void relinquishExclusive()
    {
	Thread t = Thread.currentThread();
	if (t != activeExclusive)
	    throw new IllegalStateException( t + " relinquished an exclusive lock it did not hold!" );
	else if (exclusive_exclusive_reentries > 0)
	    --exclusive_exclusive_reentries;
	else
	    {
		if ( exclusive_shared_reentries != 0 )
		    throw new IllegalStateException( t + " relinquished an exclusive lock while it had reentered but not yet relinquished shared lock acquisitions!" );
		activeExclusive = null;
		this.notifyAll();
	    }
	//status("relinquishExcusive()");
    }

    private boolean okayForShared()
    { return ( activeExclusive == null && waitingExclusive.size() == 0 ); }

    private boolean okayForExclusive( Thread t )
    { 
	// originally simple...
	//   return ( activeShared.size() == 0 && activeExclusive == null ); 
	//
	// modified to allow for exclusive reentry when unique shared lock owner

	int active_shared_sz = activeShared.size();
	if ( active_shared_sz == 0 )
	    return (activeExclusive == null); //that case where (activeExclusive == t) is special-cased above
	else if (active_shared_sz == 1)
	    return (activeShared.get(0) == t);
	else
	    {
		Set activeSharedNoDups = new HashSet( activeShared );
		return (activeSharedNoDups.size() == 1 && activeSharedNoDups.contains( t ));
	    }
    }

    public String toString()
    { return super.toString() + " [name=" + name + ']'; }
}
