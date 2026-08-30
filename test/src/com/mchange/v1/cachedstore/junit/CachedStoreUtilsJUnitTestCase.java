package com.mchange.v1.cachedstore.junit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import com.mchange.v1.cachedstore.CacheFlushException;
import com.mchange.v1.cachedstore.CachedStore;
import com.mchange.v1.cachedstore.CachedStoreException;
import com.mchange.v1.cachedstore.CachedStoreFactory;
import com.mchange.v1.cachedstore.CachedStoreUtils;
import com.mchange.v1.cachedstore.TweakableCachedStore;
import com.mchange.v1.cachedstore.WritableCachedStore;

/**
 *  Exercises the wrappers in CachedStoreUtils: plain delegation, the lock the
 *  wrappers synchronize on, the snapshot semantics of cachedKeys(), and the
 *  requirement that a store's checked exceptions reach the caller intact.
 */
public class CachedStoreUtilsJUnitTestCase extends TestCase
{
    private static List drain(Iterator ii)
    {
	List out = new ArrayList();
	while ( ii.hasNext() )
	    out.add( ii.next() );
	return out;
    }

    // --- delegation ---

    public void testSynchronizedCachedStoreDelegates() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	CachedStore store = CachedStoreUtils.synchronizedCachedStore(
	    CachedStoreFactory.createNoCleanupCachedStore( mgr ) );

	assertEquals("one", store.find("alpha"));
	assertEquals(1, mgr.recreateCount);

	store.reset();
	store.find("alpha");
	assertEquals("reset() should reach the wrapped store", 2, mgr.recreateCount);
    }

    public void testSynchronizedTweakableDelegatesEveryOperation() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	TweakableCachedStore store = CachedStoreUtils.synchronizedTweakableCachedStore(
	    CachedStoreFactory.createNoCleanupCachedStore( mgr ) );

	assertEquals("one", store.find("alpha"));

	store.setCachedValue("beta", "two");
	assertEquals("two", store.getCachedValue("beta"));

	List keys = drain( store.cachedKeys() );
	assertEquals(2, keys.size());
	assertTrue( keys.contains("alpha") );
	assertTrue( keys.contains("beta") );

	store.removeFromCache("beta");
	assertNull( store.getCachedValue("beta") );

	store.reset();
	assertEquals(0, drain( store.cachedKeys() ).size());
    }

    public void testSynchronizedWritableDelegatesEveryOperation() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreUtils.synchronizedWritableCachedStore(
	    CachedStoreFactory.createDefaultWritableCachedStore( mgr ) );

	store.write("alpha", "one");
	assertEquals("one", store.find("alpha"));

	store.flushWrites();
	assertEquals("one", mgr.storage.get("alpha"));
	assertNull( store.getFailedWrites() );

	store.write("beta", "two");
	store.clearPendingWrites();
	store.flushWrites();
	assertFalse( mgr.storage.containsKey("beta") );

	store.remove("alpha");
	store.sync();
	assertFalse( mgr.storage.containsKey("alpha") );
    }

    public void testUntweakableCachedStoreHidesTheTweakableOperations() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	TweakableCachedStore inner = CachedStoreFactory.createNoCleanupCachedStore( mgr );
	CachedStore store = CachedStoreUtils.untweakableCachedStore( inner );

	assertFalse("the tweakable operations should not be reachable by a cast",
		    store instanceof TweakableCachedStore);

	assertEquals("one", store.find("alpha"));
	assertNotNull("find() should have populated the underlying cache", inner.getCachedValue("alpha"));

	store.reset();
	assertNull("reset() should reach the wrapped store", inner.getCachedValue("alpha"));
    }

    // --- exception fidelity ---

    public void testCheckedExceptionsSurviveTheSynchronizedWrapper()
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("boom");
	TweakableCachedStore store = CachedStoreUtils.synchronizedTweakableCachedStore(
	    CachedStoreFactory.createNoCleanupCachedStore( mgr ) );

	try
	    {
		store.find("boom");
		fail("expected a CachedStoreException");
	    }
	catch (CachedStoreException e)
	    { /* expected */ }
	catch (RuntimeException e)
	    { fail("the wrapper mangled the checked exception into " + e.getClass().getName()); }
    }

    public void testCacheFlushExceptionSurvivesTheSynchronizedWrapper() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("bad");
	WritableCachedStore store = CachedStoreUtils.synchronizedWritableCachedStore(
	    CachedStoreFactory.createDefaultWritableCachedStore( mgr ) );

	store.write("bad", "one");
	try
	    {
		store.flushWrites();
		fail("expected a CacheFlushException");
	    }
	catch (CacheFlushException e)
	    { /* expected */ }
	catch (RuntimeException e)
	    { fail("the wrapper mangled the checked exception into " + e.getClass().getName()); }
    }

    // --- cachedKeys() snapshot semantics ---

    public void testCachedKeysIsASnapshotNotALiveView() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreUtils.synchronizedTweakableCachedStore(
	    CachedStoreFactory.createNoCleanupCachedStore( mgr ) );

	store.setCachedValue("alpha", "one");
	store.setCachedValue("beta", "two");

	Iterator ii = store.cachedKeys();

	// a live, fail-fast iterator over the backing map would blow up on the drain below
	store.setCachedValue("gamma", "three");
	store.removeFromCache("alpha");

	List keys = drain( ii );
	assertEquals("the snapshot should reflect the cache as of the cachedKeys() call", 2, keys.size());
	assertTrue( keys.contains("alpha") );
	assertTrue( keys.contains("beta") );
    }

    public void testCachedKeysIteratorRefusesRemove() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreUtils.synchronizedTweakableCachedStore(
	    CachedStoreFactory.createNoCleanupCachedStore( mgr ) );

	store.setCachedValue("alpha", "one");

	Iterator ii = store.cachedKeys();
	assertTrue( ii.hasNext() );
	ii.next();
	try
	    {
		ii.remove();
		fail("removing through the snapshot would not reach the store, so it should be refused");
	    }
	catch (UnsupportedOperationException e)
	    { /* expected */ }

	assertNotNull("the store should be untouched", store.getCachedValue("alpha"));
    }

    public void testGetFailedWritesIsASnapshotThroughTheWrapper() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("bad");
	WritableCachedStore store = CachedStoreUtils.synchronizedWritableCachedStore(
	    CachedStoreFactory.createDefaultWritableCachedStore( mgr ) );

	store.write("bad", "one");
	try { store.flushWrites(); } catch (CacheFlushException e) { /* expected */ }

	Set snapshot = store.getFailedWrites();
	assertEquals(1, snapshot.size());

	mgr.failKeys.clear();
	store.flushWrites();

	assertNull( store.getFailedWrites() );
	assertEquals("a previously returned Set must not change underneath its caller", 1, snapshot.size());
    }

    // --- locking ---

    public void testWrapperSynchronizesOnItself() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	final CachedStore store = CachedStoreUtils.synchronizedCachedStore(
	    CachedStoreFactory.createNoCleanupCachedStore( mgr ) );

	final AtomicBoolean completed = new AtomicBoolean( false );
	Thread t = new Thread( new Runnable()
	    {
		public void run()
		{
		    try { store.find("alpha"); completed.set( true ); }
		    catch (Exception e) { e.printStackTrace(); }
		}
	    } );

	synchronized ( store )
	    {
		t.start();
		t.join( 250 );
		assertFalse("holding the wrapper's own monitor should block its operations", completed.get());
	    }

	t.join( 10000 );
	assertTrue("the operation should complete once the monitor is released", completed.get());
    }

    public void testConcurrentIterationAndMutationAreSafe() throws Exception
    {
	TestManager mgr = new TestManager();
	final TweakableCachedStore store = CachedStoreUtils.synchronizedTweakableCachedStore(
	    CachedStoreFactory.createNoCleanupCachedStore( mgr ) );

	for (int i = 0; i < 100; ++i)
	    store.setCachedValue("k" + i, "v" + i);

	final List errors = Collections.synchronizedList( new ArrayList() );
	final long deadline = System.currentTimeMillis() + 1000;

	Runnable mutator = new Runnable()
	    {
		public void run()
		{
		    try
			{
			    int i = 1000;
			    while ( System.currentTimeMillis() < deadline )
				{
				    store.setCachedValue("k" + i, "v");
				    store.removeFromCache("k" + i);
				    ++i;
				}
			}
		    catch (Throwable t) { errors.add( t ); }
		}
	    };

	Runnable reader = new Runnable()
	    {
		public void run()
		{
		    try
			{
			    while ( System.currentTimeMillis() < deadline )
				drain( store.cachedKeys() );
			}
		    catch (Throwable t) { errors.add( t ); }
		}
	    };

	Thread[] threads = new Thread[]
	    { new Thread( mutator ), new Thread( mutator ), new Thread( reader ), new Thread( reader ) };

	for (int i = 0; i < threads.length; ++i) threads[i].start();
	for (int i = 0; i < threads.length; ++i) threads[i].join();

	if ( !errors.isEmpty() )
	    fail("concurrent iteration and mutation failed: " + errors.get(0));
    }
}
