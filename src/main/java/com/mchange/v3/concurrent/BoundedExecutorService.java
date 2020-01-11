package com.mchange.v3.concurrent;

import java.util.*;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;

import static com.mchange.v3.concurrent.BoundedExecutorService.State.*;

// inspired by Java Concurrency In Practice, Goetz et al, Listing 8.4
// thanks to Julia Evans, http://jvns.ca/blog/2016/03/29/thread-pools-part-ii-i-love-blocking/
// for the tip!

// TODO: Seriously test this.

public final class BoundedExecutorService extends AbstractExecutorService {

    final static MLogger logger = MLog.getLogger( BoundedExecutorService.class );

    enum State { ACCEPTING, SATURATED, UNWINDING, SHUTDOWN, SHUTDOWN_NOW }

    //MT: Thread safe
    final ExecutorService inner;
    final int             blockBound;
    final int             restartBeneath;

    //MT: protected by this' lock
    State state;
    int   permits;

    Map<Thread,Runnable> waiters   = new HashMap<Thread,Runnable>();

    public BoundedExecutorService( ExecutorService inner, int blockBound, int restartBeneath )
    {
	if ( blockBound <= 0 || restartBeneath <= 0 )
	    throw new IllegalArgumentException( "blockBound and restartBeneath must both be greater than zero!" );
	if ( restartBeneath > blockBound )
	    throw new IllegalArgumentException( "restartBeneath must be less than or equal to blockBound!" );

	this.inner = inner;
	this.blockBound = blockBound;
	this.restartBeneath = restartBeneath;

	this.state   = ACCEPTING;
	this.permits = 0;
    } 

    public BoundedExecutorService( ExecutorService inner, int blockBound )
    { this( inner, blockBound, blockBound ); }

    public synchronized State getState()
    { return state; }

    public synchronized boolean isShutdown()
    { return state == SHUTDOWN || state == SHUTDOWN_NOW; }

    public synchronized boolean isTerminated()
    { return isShutdown() && permits == 0; }

    public synchronized void shutdown()
    {
	inner.shutdown();

	updateState( SHUTDOWN );
	this.notifyAll();
    }

    public synchronized List<Runnable> shutdownNow()
    {
	updateState( SHUTDOWN_NOW );

	List<Runnable> innerLeftovers = inner.shutdownNow();
	Collection<Runnable> ourLeftovers = waiters.values();

	List<Runnable> out = new ArrayList<Runnable>( innerLeftovers.size() + ourLeftovers.size() );
	out.addAll( innerLeftovers );
	out.addAll( ourLeftovers );

	// can't do it because -source 1.6 is still set
	//waiters.keySet().stream().forEach( t -> t.interrupt() );

	for ( Iterator<Thread> ii = waiters.keySet().iterator(); ii.hasNext(); ) ii.next().interrupt();

	waiters.clear();
	    
	return Collections.unmodifiableList( out );
    }

    public synchronized boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
    {
	long start = System.currentTimeMillis();
	long timeoutMillis = start + TimeUnit.MILLISECONDS.convert( timeout, unit ); 

	boolean innerTerminated = inner.awaitTermination( timeout, unit );
	if ( innerTerminated )
	{
	    long now = System.currentTimeMillis();
	    while (!isTerminated()) 
	    {
		if ( now > timeoutMillis ) return false;
		this.wait( timeoutMillis - now );
	    }
	    return true;
	}
	else
        {
	    return false;
	}
    }

    //MT: no need to synchronize
    public void execute( Runnable runnable )
    { inner.execute( newTaskFor( runnable, null ) ); }

    //MT: no need to synchronize
    protected <V> RunnableFuture<V> newTaskFor(Callable<V> callable) {
	PermitAcquiringCallable<V> pac = new PermitAcquiringCallable<V>( callable );
	ReleasingFutureTask<V>     rft = new ReleasingFutureTask<V>( pac );
	pac.setTask( rft );
	return rft;
    }

    //MT: no need to synchronize
    protected <V> RunnableFuture<V> newTaskFor(Runnable runnable, V result) {
	PermitAcquiringRunnable<V> par = new PermitAcquiringRunnable<V>( runnable );
	ReleasingFutureTask<V>     rft = new ReleasingFutureTask<V>( par, result );
	par.setTask( rft );
	return rft;
    }

    // MT: Call only with this' lock
    private boolean shouldWait() 
    {
	switch ( state ) 
	{
	case SHUTDOWN:
	case SHUTDOWN_NOW:
	    return permits == blockBound; // permits will be zero in SHUTDOWN_NOW, and blockBound > 0, so we'll never wait()
	case ACCEPTING:
	    return false;
	case SATURATED:
	case UNWINDING:
	    return true;
	default:
	    throw new AssertionError("This should be dead code.");
	}
    }

    private synchronized void acquirePermit( Runnable task )
    {
	try 
	{
	    switch ( state ) 
	    {
	    case SHUTDOWN:
	    case SHUTDOWN_NOW:
		throw new RejectedExecutionException( this + " has been shut down. [state=" + state + "]" );
	    case ACCEPTING:
	    case SATURATED:
	    case UNWINDING:
		while ( shouldWait() ) 
		{
		    try 
		    {
			waiters.put( Thread.currentThread(), task );
			this.wait();
		    }
		    finally
		    { waiters.remove( Thread.currentThread() ); }
		}
	    
		if ( state != SHUTDOWN_NOW )
		{
		    ++permits;
		    if ( permits == blockBound ) updateState( SATURATED );
		}
	    }
	}
	catch ( InterruptedException e )
	{
	    throw new RejectedExecutionException( this + " has been forcibly shut down. [state=" + state + "]", e );
	}
    }

    private synchronized void releasePermit()
    {
	--permits;

	if ( permits < restartBeneath )
	{
	    updateState( ACCEPTING );
	}
	else if ( state == SATURATED && permits < blockBound )
	{
	    updateState( UNWINDING );
	}
    }

    // MT: call only from methods holding this' lock
    private void updateState( State newState )
    {
	switch ( this.state )
	{
	case ACCEPTING:
	case SATURATED:
	case UNWINDING:
	    if ( this.state != newState ) doUpdateState( newState );
	    break;
	case SHUTDOWN:
	    if ( newState == SHUTDOWN_NOW ) doUpdateState( newState );
	    break;
	case SHUTDOWN_NOW:
	    // can't change states from SHUTDOWN_NOW
	}
    }

    // MT: call only from methods holding this' lock
    private void doUpdateState( State newState )
    {
	if (logger.isLoggable( MLevel.FINE ))
	    logger.log(MLevel.FINE, "State transition " + this.state + " => " + newState + "; blockBound=" + blockBound + "; restartBeneath=" + restartBeneath + "; permits=" + permits );
	
	this.state = newState;
	if ( this.state == SHUTDOWN_NOW ) this.permits = 0;
	this.notifyAll();

    }

    private final class PermitAcquiringCallable<V> implements Callable<V>, DelayedTaskSettable<V>
    {
	Callable<V>            callable;
	ReleasingFutureTask<V> task;
	    
	PermitAcquiringCallable( Callable<V> callable )
	{
	    this.callable = callable;
	}

	public void setTask( ReleasingFutureTask<V> task )
	{
	    this.task = task;
	}
	    
	public V call() throws Exception
	{
	    acquirePermit( this.task );
	    return callable.call();
	}
    }
    
    private final class PermitAcquiringRunnable<V> implements Runnable, DelayedTaskSettable<V>
    {
	Runnable               runnable;
	ReleasingFutureTask<V> task;
	
	PermitAcquiringRunnable( Runnable runnable )
	{
	    this.runnable = runnable;
	}
	
	public void setTask( ReleasingFutureTask<V> task )
	{
	    this.task = task;
	}

	public void run()
	{
	    acquirePermit( this.task );
	    runnable.run();
	}
    }

    private interface DelayedTaskSettable<V>
    {
	public void setTask( ReleasingFutureTask<V> task );
    }

    private final class ReleasingFutureTask<V> extends FutureTask<V>
    {
	
	ReleasingFutureTask(PermitAcquiringCallable<V> callable)
	{
	    super( callable );
	}

	ReleasingFutureTask(PermitAcquiringRunnable<V> runnable, V result)
	{ super( runnable, result ); }

	protected void done() 
	{ releasePermit(); }
    }
}
