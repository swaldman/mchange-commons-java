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

/**
 * NONREENTRANT!!! 
 * also... would be more efficient if we
 * segregated readers and writers into 
 * separate wait sets.
 */
public class SimpleSharedUseExclusiveUseLock implements SharedUseExclusiveUseLock
{
    private int waiting_readers = 0;
    private int active_readers  = 0;

    private int     waiting_writers = 0;
    private boolean writer_active   = false;

    public synchronized void acquireShared() throws InterruptedException
    {
	try
	    {
		++waiting_readers;
		while (! okayToRead())
		    this.wait();
		++active_readers;
	    }
	finally
	    { 
		--waiting_readers; 
	    }
    }

    public synchronized void relinquishShared()
    { 
	--active_readers; 
	this.notifyAll();
    }

    public synchronized void acquireExclusive() throws InterruptedException
    {
	try
	    {
		++waiting_writers;
		while (! okayToWrite())
		    this.wait();
		writer_active = true;
	    }
	finally
	    {
		--waiting_writers;
	    }
    }

    public synchronized void relinquishExclusive()
    {
	writer_active = false;
	this.notifyAll();
    }

    private boolean okayToRead()
    { return (!writer_active && waiting_writers == 0); }

    private boolean okayToWrite()
    { return (active_readers == 0 && !writer_active); }
}
