package com.mchange.v1.cachedstore.junit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import com.mchange.v1.cachedstore.CachedStoreException;
import com.mchange.v1.cachedstore.CachedStoreFactory;
import com.mchange.v1.cachedstore.TweakableCachedStore;

/**
 *  Exercises the read-only CachedStores handed out by CachedStoreFactory.
 *
 *  Several of these stores print stack traces on expected failures (the
 *  package's DEBUG flags are compiled on), so failure-path tests are
 *  deliberately noisy on stderr.
 */
public class CachedStoreFactoryJUnitTestCase extends TestCase
{
    private static List drain(Iterator ii)
    {
	List out = new ArrayList();
	while ( ii.hasNext() )
	    out.add( ii.next() );
	return out;
    }

    // --- createNoCleanupCachedStore ---

    public void testNoCleanupCachesAfterFirstFind() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	TweakableCachedStore store = CachedStoreFactory.createNoCleanupCachedStore( mgr );

	assertEquals("one", store.find("alpha"));
	assertEquals("one", store.find("alpha"));
	assertEquals("value should be recreated from the manager only once", 1, mgr.recreateCount);
    }

    public void testNoCleanupRecreatesWhenManagerReportsDirty() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	TweakableCachedStore store = CachedStoreFactory.createNoCleanupCachedStore( mgr );

	assertEquals("one", store.find("alpha"));

	mgr.dirtyKeys.add("alpha");
	mgr.storage.put("alpha", "two");

	assertEquals("a dirty value should be recreated", "two", store.find("alpha"));
	assertEquals(2, mgr.recreateCount);
    }

    public void testNoCleanupResetDiscardsCachedValues() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	TweakableCachedStore store = CachedStoreFactory.createNoCleanupCachedStore( mgr );

	store.find("alpha");
	assertEquals(1, drain( store.cachedKeys() ).size());

	store.reset();
	assertEquals("reset() should empty the cache", 0, drain( store.cachedKeys() ).size());

	store.find("alpha");
	assertEquals("reset() should force a recreate on the next find", 2, mgr.recreateCount);
    }

    public void testNoCleanupUnknownKeyYieldsNullAndIsNotCached() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreFactory.createNoCleanupCachedStore( mgr );

	assertNull( store.find("absent") );
	assertNull( store.getCachedValue("absent") );
	assertEquals("a null recreate should leave nothing cached", 0, drain( store.cachedKeys() ).size());

	store.find("absent");
	assertEquals("an uncached key should be recreated on every find", 2, mgr.recreateCount);
    }

    public void testNoCleanupTweakableRoundTrip() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreFactory.createNoCleanupCachedStore( mgr );

	assertNull("getCachedValue should be null for an uncached key", store.getCachedValue("alpha"));

	store.setCachedValue("alpha", "one");
	assertEquals("one", store.getCachedValue("alpha"));
	assertEquals("a hand-seeded value should satisfy find without the manager",
		     "one", store.find("alpha"));
	assertEquals(0, mgr.recreateCount);

	store.removeFromCache("alpha");
	assertNull( store.getCachedValue("alpha") );
    }

    public void testNoCleanupCachedKeysReflectsCacheContents() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreFactory.createNoCleanupCachedStore( mgr );

	store.setCachedValue("alpha", "one");
	store.setCachedValue("beta", "two");

	List keys = drain( store.cachedKeys() );
	assertEquals(2, keys.size());
	assertTrue( keys.contains("alpha") );
	assertTrue( keys.contains("beta") );
    }

    public void testNoCleanupManagerFailureBecomesCachedStoreException()
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("boom");
	TweakableCachedStore store = CachedStoreFactory.createNoCleanupCachedStore( mgr );

	try
	    {
		store.find("boom");
		fail("a failing manager should yield a CachedStoreException");
	    }
	catch (CachedStoreException e)
	    { assertNotNull("the underlying failure should be retained as the cause", e.getCause()); }
    }

    // --- createNoCacheCachedStore ---

    public void testNoCacheConsultsManagerOnEveryFind() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	TweakableCachedStore store = CachedStoreFactory.createNoCacheCachedStore( mgr );

	assertEquals("one", store.find("alpha"));
	assertEquals("one", store.find("alpha"));
	assertEquals("nothing should be cached, so every find recreates", 2, mgr.recreateCount);
    }

    public void testNoCacheRetainsNothing() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreFactory.createNoCacheCachedStore( mgr );

	store.setCachedValue("alpha", "one");

	assertNull("setCachedValue should be a no-op", store.getCachedValue("alpha"));
	assertEquals("cachedKeys should always be empty", 0, drain( store.cachedKeys() ).size());

	store.removeFromCache("alpha");
	store.reset();
    }

    // --- createSoftValueCachedStore ---

    public void testSoftValueCachesAfterFirstFind() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	TweakableCachedStore store = CachedStoreFactory.createSoftValueCachedStore( mgr );

	assertEquals("one", store.find("alpha"));
	assertEquals("one", store.find("alpha"));
	assertEquals(1, mgr.recreateCount);
    }

    public void testSoftValueTweakableRoundTrip() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreFactory.createSoftValueCachedStore( mgr );

	store.setCachedValue("alpha", "one");
	assertEquals("the soft reference should be unwrapped on the way out",
		     "one", store.getCachedValue("alpha"));

	store.removeFromCache("alpha");
	assertNull( store.getCachedValue("alpha") );
    }

    // --- createSynchronousCleanupSoftKeyCachedStore ---

    public void testSoftKeyCachesAfterFirstFind() throws Exception
    {
	TestManager mgr = new TestManager();
	mgr.storage.put("alpha", "one");
	TweakableCachedStore store = CachedStoreFactory.createSynchronousCleanupSoftKeyCachedStore( mgr );

	assertEquals("one", store.find("alpha"));
	assertEquals("one", store.find("alpha"));
	assertEquals(1, mgr.recreateCount);
    }

    public void testSoftKeyTweakableRoundTrip() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreFactory.createSynchronousCleanupSoftKeyCachedStore( mgr );

	store.setCachedValue("alpha", "one");
	assertEquals("one", store.getCachedValue("alpha"));

	store.removeFromCache("alpha");
	assertNull( store.getCachedValue("alpha") );
    }

    public void testSoftKeyCachedKeysExposesUserKeysNotSoftKeys() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreFactory.createSynchronousCleanupSoftKeyCachedStore( mgr );

	store.setCachedValue("alpha", "one");

	List keys = drain( store.cachedKeys() );
	assertEquals(1, keys.size());
	assertEquals("the soft key should be unwrapped back to the user's key", "alpha", keys.get(0));
    }

    public void testSoftKeyCachedKeysIteratorRefusesRemove() throws Exception
    {
	TestManager mgr = new TestManager();
	TweakableCachedStore store = CachedStoreFactory.createSynchronousCleanupSoftKeyCachedStore( mgr );

	store.setCachedValue("alpha", "one");

	Iterator ii = store.cachedKeys();
	assertTrue( ii.hasNext() );
	ii.next();
	try
	    {
		ii.remove();
		fail("this iterator is documented not to support remove()");
	    }
	catch (UnsupportedOperationException e)
	    { /* expected */ }
    }

    public void testSoftKeyManagerFailureBecomesCachedStoreException()
    {
	TestManager mgr = new TestManager();
	mgr.failKeys.add("boom");
	TweakableCachedStore store = CachedStoreFactory.createSynchronousCleanupSoftKeyCachedStore( mgr );

	try
	    {
		store.find("boom");
		fail("a failing manager should yield a CachedStoreException");
	    }
	catch (CachedStoreException e)
	    { /* expected */ }
	catch (RuntimeException e)
	    { fail("the checked exception was mangled into " + e.getClass().getName()); }
    }
}
