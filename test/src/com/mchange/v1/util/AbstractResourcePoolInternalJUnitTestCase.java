package com.mchange.v1.util;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 *  Lives in com.mchange.v1.util rather than a junit subpackage because everything
 *  AbstractResourcePool exposes to a subclass -- init, close, checkoutResource -- is
 *  protected.
 *
 *  <p>close() and unexpectedBreak() iterated a collection while removeResource() removed
 *  from it, through the collection rather than through the iterator. The failure was not a
 *  clean ConcurrentModificationException: the CME comes out of next(), which sits inside the
 *  try, while hasNext() sits in the loop condition outside it, so catching the exception put
 *  the loop straight back where it was. Measured under JDK 11, a HashSet spins forever from
 *  two elements on. Either way at most one resource was ever destroyed.</p>
 *
 *  <p>Because the failure was a hang rather than a wrong answer, close() is driven on a
 *  worker thread and joined with a timeout -- otherwise a regression would wedge the whole
 *  suite instead of failing this test.</p>
 */
public class AbstractResourcePoolInternalJUnitTestCase extends TestCase
{
    private final static long CLOSE_TIMEOUT_MS = 10000;

    /**
     *  A pool over Integer resources that records every destroyResource call, so a test can
     *  assert that close() actually reached all of them.
     */
    private static class CountingPool extends AbstractResourcePool
    {
        final List<Object> destroyed = new ArrayList<Object>();
        int nextResource = 0;

        CountingPool(int start) throws Exception
        {
            super( start, start, 1 );
            init();
        }

        @Override
        protected Object acquireResource()
        { return Integer.valueOf( nextResource++ ); }

        @Override
        protected void refurbishResource(Object resc)
        {}

        @Override
        protected synchronized void destroyResource(Object resc)
        { destroyed.add( resc ); }

        // close() is protected; expose it so a test can call it from a worker thread
        void closeFromTest() throws Exception
        { this.close(); }

        synchronized int managedCount()
        { return managed.size(); }

        synchronized List<Object> destroyedSnapshot()
        { return new ArrayList<Object>( destroyed ); }
    }

    /**
     *  Runs pool.closeFromTest() on a worker thread, failing rather than hanging if it does
     *  not return. Returns whatever the close threw, or null.
     */
    private Throwable closeWithTimeout(final CountingPool pool) throws InterruptedException
    {
        final Throwable[] thrown = new Throwable[1];
        Thread t = new Thread("AbstractResourcePool-close")
        {
            @Override
            public void run()
            {
                try { pool.closeFromTest(); }
                catch ( Throwable e ) { thrown[0] = e; }
            }
        };
        t.setDaemon( true );
        t.start();
        t.join( CLOSE_TIMEOUT_MS );

        if ( t.isAlive() )
            fail( "close() did not return within " + CLOSE_TIMEOUT_MS +
                  "ms -- it is iterating a collection it is removing from." );

        return thrown[0];
    }

    /** The single-resource case, which the defect happened not to break. */
    public void testCloseDestroysSoleResource() throws Exception
    {
        CountingPool pool = new CountingPool( 1 );
        assertEquals( 1, pool.managedCount() );

        assertNull( closeWithTimeout( pool ) );
        assertEquals( "close() must destroy the one resource it manages.", 1, pool.destroyedSnapshot().size() );
    }

    /**
     *  Two resources is where a HashSet begins to spin. Under the defect this test hangs and
     *  is failed by the timeout; before the timeout was added it would have hung the suite.
     */
    public void testCloseDestroysEveryResourceWhenThereAreTwo() throws Exception
    {
        CountingPool pool = new CountingPool( 2 );
        assertEquals( 2, pool.managedCount() );

        assertNull( closeWithTimeout( pool ) );
        assertEquals( "close() must destroy every resource, not just the first.",
                      2, pool.destroyedSnapshot().size() );
    }

    /** and a larger pool, so the assertion is about "all of them" rather than a boundary. */
    public void testCloseDestroysEveryResourceInALargerPool() throws Exception
    {
        CountingPool pool = new CountingPool( 8 );
        assertEquals( 8, pool.managedCount() );

        assertNull( closeWithTimeout( pool ) );

        List<Object> destroyed = pool.destroyedSnapshot();
        assertEquals( "close() must destroy every resource the pool manages.", 8, destroyed.size() );
        for ( int i = 0; i < 8; ++i )
            assertTrue( "Resource " + i + " must have been destroyed.", destroyed.contains( Integer.valueOf(i) ) );
        assertEquals( "close() must leave nothing managed.", 0, pool.managedCount() );
    }
}
