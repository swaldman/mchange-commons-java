package com.mchange.v1.cachedstore.junit;

import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import com.mchange.v1.cachedstore.Autoflushing;
import com.mchange.v1.cachedstore.CacheFlushException;
import com.mchange.v1.cachedstore.CachedStoreFactory;
import com.mchange.v1.cachedstore.WritableCachedStore;

/**
 *  Exercises the WritableCachedStores handed out by CachedStoreFactory:
 *  read-your-writes before a flush, write-through on flush, and the
 *  failed-write bookkeeping described on WritableCachedStore.
 */
public class WritableCachedStoreJUnitTestCase extends TestCase
{
    // --- createDefaultWritableCachedStore: pending writes ---

    public void testWriteIsVisibleBeforeFlushButNotInStorage() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("alpha", "one");

	assertEquals("a pending write should be readable immediately", "one", store.find("alpha"));
	assertFalse("a pending write should not have reached storage", mgr.storage.containsKey("alpha"));
	assertEquals("a pending write should not consult the manager", 0, mgr.recreateCount);
    }

    public void testFlushWritesReachStorage() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("alpha", "one");
	store.flushWrites();

	assertEquals("one", mgr.storage.get("alpha"));
	assertEquals(1, mgr.writeCount);
    }

    public void testRemoveIsVisibleBeforeFlushButNotInStorage() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.remove("alpha");

	assertNull("a pending remove should hide the value", store.find("alpha"));
	assertTrue("a pending remove should not have reached storage", mgr.storage.containsKey("alpha"));
    }

    public void testFlushedRemoveReachesStorage() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.remove("alpha");
	store.flushWrites();

	assertFalse( mgr.storage.containsKey("alpha") );
	assertEquals(1, mgr.removeCount);
	assertNull("the value should still read as absent after the flush", store.find("alpha"));
    }

    public void testFlushedRemoveEvictsTheReadCache() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	// the first flush seeds the read cache; the flushed remove must later clear it
	store.write("alpha", "one");
	store.flushWrites();

	store.remove("alpha");
	store.flushWrites();

	assertFalse( mgr.storage.containsKey("alpha") );

	int recreatesBefore = mgr.recreateCount;
	assertNull("a flushed remove must not leave the old value readable", store.find("alpha"));
	assertEquals("the read cache should have been evicted, forcing a recreate",
		     recreatesBefore + 1, mgr.recreateCount);
    }

    public void testFlushedRemoveNeverShowsStoreInternalsToTheManager() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("alpha", "one");
	store.flushWrites();
	store.remove("alpha");
	store.flushWrites();
	store.find("alpha");

	for (Iterator ii = mgr.isDirtyCachedValues.iterator(); ii.hasNext(); )
	    {
		Object cached = ii.next();
		assertTrue("isDirty() was handed a value the manager never produced: " + cached,
			   cached instanceof String);
	    }
    }

    public void testLastWriteWins() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("alpha", "one");
	store.write("alpha", "two");
	store.flushWrites();

	assertEquals("two", mgr.storage.get("alpha"));
    }

    // --- failed writes ---

    public void testFailedFlushThrowsAndReportsTheKey() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("bad");
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("bad", "one");
	try
	    {
		store.flushWrites();
		fail("a failing back-end write should yield a CacheFlushException");
	    }
	catch (CacheFlushException e)
	    { /* expected */ }

	Set failed = store.getFailedWrites();
	assertNotNull( failed );
	assertEquals(1, failed.size());
	assertTrue( failed.contains("bad") );
	assertFalse( mgr.storage.containsKey("bad") );
    }

    public void testFailedWriteRemainsPendingAndReadable() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("bad");
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("bad", "one");
	try { store.flushWrites(); } catch (CacheFlushException e) { /* expected */ }

	assertEquals("a failed write should still be readable from the cache", "one", store.find("bad"));
    }

    public void testFailedWriteIsRetriedOnTheNextFlush() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("bad");
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("bad", "one");
	try { store.flushWrites(); } catch (CacheFlushException e) { /* expected */ }

	mgr.failKeys.clear();
	store.flushWrites();

	assertEquals("one", mgr.storage.get("bad"));
	assertNull("a successful retry should clear the failed writes", store.getFailedWrites());
    }

    public void testGetFailedWritesIsNullWhenNothingHasFailed() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	assertNull( store.getFailedWrites() );

	store.write("alpha", "one");
	store.flushWrites();

	assertNull( store.getFailedWrites() );
    }

    public void testGetFailedWritesIsUnmodifiable() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("bad");
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("bad", "one");
	try { store.flushWrites(); } catch (CacheFlushException e) { /* expected */ }

	Set failed = store.getFailedWrites();
	try
	    {
		failed.add("sneaky");
		fail("getFailedWrites() is documented to return an unmodifiable Set");
	    }
	catch (UnsupportedOperationException e)
	    { /* expected */ }
    }

    public void testGetFailedWritesIsASnapshot() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("bad");
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("bad", "one");
	try { store.flushWrites(); } catch (CacheFlushException e) { /* expected */ }

	Set snapshot = store.getFailedWrites();
	assertEquals(1, snapshot.size());

	// let the write succeed, which empties the store's internal failedWrites set
	mgr.failKeys.clear();
	store.flushWrites();

	assertNull( store.getFailedWrites() );
	assertEquals("a previously returned Set must not change underneath its caller", 1, snapshot.size());
	assertTrue( snapshot.contains("bad") );
    }

    public void testClearPendingWritesDiscardsWritesAndFailures() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("bad");
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("bad", "one");
	try { store.flushWrites(); } catch (CacheFlushException e) { /* expected */ }

	store.clearPendingWrites();
	assertNull( store.getFailedWrites() );

	mgr.failKeys.clear();
	store.flushWrites();
	assertFalse("a cleared write should never reach storage", mgr.storage.containsKey("bad"));
    }

    // --- reset and sync ---

    public void testResetDiscardsPendingWrites() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.write("alpha", "one");
	store.reset();
	store.flushWrites();

	assertFalse("reset() discards pending writes without writing them", mgr.storage.containsKey("alpha"));
	assertNull( store.find("alpha") );
    }

    public void testSyncFlushesThenClearsTheReadCache() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	WritableCachedStore store = CachedStoreFactory.createDefaultWritableCachedStore( mgr );

	store.find("alpha");
	int recreatesBefore = mgr.recreateCount;

	store.write("beta", "two");
	store.sync();

	assertEquals("sync() should flush pending writes", "two", mgr.storage.get("beta"));

	store.find("alpha");
	assertEquals("sync() should clear cached reads", recreatesBefore + 1, mgr.recreateCount);
    }

    // --- cacheWritesOnlyWritableCachedStore ---

    public void testCacheWritesOnlyReadsAlwaysConsultTheManager() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	WritableCachedStore store = CachedStoreFactory.cacheWritesOnlyWritableCachedStore( mgr );

	store.find("alpha");
	store.find("alpha");

	assertEquals("reads are not cached by this store", 2, mgr.recreateCount);
    }

    public void testCacheWritesOnlyStillBuffersWrites() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.cacheWritesOnlyWritableCachedStore( mgr );

	store.write("alpha", "one");
	assertEquals("one", store.find("alpha"));
	assertFalse( mgr.storage.containsKey("alpha") );

	store.flushWrites();
	assertEquals("one", mgr.storage.get("alpha"));
    }

    // --- createNoCacheWritableCachedStore ---

    public void testNoCacheWritableWritesStraightThrough() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createNoCacheWritableCachedStore( mgr );

	store.write("alpha", "one");

	assertEquals("this store writes through without waiting for a flush", "one", mgr.storage.get("alpha"));
	assertEquals(1, mgr.writeCount);
    }

    public void testNoCacheWritableRemovesStraightThrough() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	WritableCachedStore store = CachedStoreFactory.createNoCacheWritableCachedStore( mgr );

	store.remove("alpha");

	assertFalse( mgr.storage.containsKey("alpha") );
	assertEquals(1, mgr.removeCount);
    }

    public void testNoCacheWritableIsAutoflushing()
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createNoCacheWritableCachedStore( mgr );

	assertTrue("every write is flushed, so this store is Autoflushing", store instanceof Autoflushing);
    }

    public void testNoCacheWritableNeverReportsFailedWrites() throws Exception
    {
	TestManager mgr = new TestManager();
	WritableCachedStore store = CachedStoreFactory.createNoCacheWritableCachedStore( mgr );

	store.write("alpha", "one");
	store.flushWrites();

	assertNull("null means every write succeeded", store.getFailedWrites());
    }

    public void testNoCacheWritableReadsAlwaysConsultTheManager() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	WritableCachedStore store = CachedStoreFactory.createNoCacheWritableCachedStore( mgr );

	assertEquals("one", store.find("alpha"));
	assertEquals("one", store.find("alpha"));
	assertEquals(2, mgr.recreateCount);
    }
}
