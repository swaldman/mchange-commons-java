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
