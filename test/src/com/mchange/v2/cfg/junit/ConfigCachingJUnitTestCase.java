package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  Caching semantics of the MConfig facades.
 *
 *  The cache is keyed on the RESOLVED resource-path list, and both facades share one
 *  store -- so two calls that resolve to the same list share an instance regardless of
 *  which facade made them.
 */
public final class ConfigCachingJUnitTestCase extends TestCase
{
    private final static String A = "/a.properties";
    private final static String B = "/b.properties";

    /** Repeated cached reads of the same paths return the identical instance. */
    public void testCachedReadsAreShared()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object first  = s.asProvidedCached( new String[] { A, B } );
            Object second = s.asProvidedCached( new String[] { A, B } );
            assertSame( first, second );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Uncached reads return a fresh instance every time. */
    public void testUncachedReadsAreNotShared()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object first  = s.asProvidedUncached( new String[] { A, B } );
            Object second = s.asProvidedUncached( new String[] { A, B } );

            assertTrue( "uncached reads should produce distinct instances", first != second );
            assertEquals( "but equivalent content",
                          s.getProperty( first, "shared" ), s.getProperty( second, "shared" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Different path lists are cached separately. */
    public void testDifferentPathListsAreCachedSeparately()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object ab = s.asProvidedCached( new String[] { A, B } );
            Object ba = s.asProvidedCached( new String[] { B, A } );

            assertTrue( ab != ba );
            assertEquals( "from-b", s.getProperty( ab, "shared" ) );
            assertEquals( "from-a", s.getProperty( ba, "shared" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  The key is the RESOLVED list, so path lists that differ only in ways condensing
     *  erases -- here a duplicate entry -- hit the same cache entry.
     */
    public void testCacheKeyIsTheResolvedPathList()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object viaDuplicate = s.asProvidedCached( new String[] { B, A, B } ); // resolves to [A, B]
            Object direct       = s.asProvidedCached( new String[] { A, B } );
            assertSame( "condensing should make these the same cache key", direct, viaDuplicate );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  The two facades no longer share a cache entry, even for an identical resolved path list.
     *
     *  PathsKey now carries MConfig.Kind alongside the paths, because the kinds do not agree about
     *  what a read of those paths MEANS: a veto aborts an AsProvidedVetoable read, is ignored with a
     *  warning by a Traditional one, and should be impossible in an AsProvided one. Handing a cached
     *  result from one kind to a caller who asked for another would hand them the wrong semantics
     *  along with the right properties.
     */
    public void testFacadesDoNotShareCacheEntries()
    {
        CfgScenario s = CfgScenario.open( "no-pathfiles" );
        try
        {
            // WithTraditionalDefaultSources with no caller paths resolves to the backstop list
            Object traditional = s.traditionalCached();

            // naming that same resolved list explicitly through AsProvided
            Object asProvided = s.asProvidedCached( new String[] {
                "/mchange-commons.properties", "hocon:/reference,/application,/", "/" } );

            assertTrue( "the resolved paths agree",
                        Arrays.equals( s.getPropertiesResourcePaths( traditional ),
                                       s.getPropertiesResourcePaths( asProvided ) ) );
            assertTrue( "but the kinds differ, so the cache entries must differ",
                        traditional != asProvided );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  Delayed log items produced while RESOLVING paths reach the caller's out-list on
     *  both a cache miss and a subsequent cache hit.
     */
    public void testResolutionLogItemsReachCallerOnMissAndHit()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            List miss = new ArrayList();
            s.asProvidedCached( new String[] { A, B }, miss );
            List<String[]> missItems = s.renderLogItems( miss );
            assertTrue( "expected the path-list notice on a cache miss, got:" + CfgScenario.describe( missItems ),
                        CfgScenario.hasLogItem( missItems, "FINER", "Reading classloader-resource-based config for path list" ) );

            List hit = new ArrayList();
            s.asProvidedCached( new String[] { A, B }, hit );
            List<String[]> hitItems = s.renderLogItems( hit );
            assertTrue( "expected the same notice on a cache hit, got:" + CfgScenario.describe( hitItems ),
                        CfgScenario.hasLogItem( hitItems, "FINER", "Reading classloader-resource-based config for path list" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  Known asymmetry, pinned so a future change to it is deliberate: parse-time items
     *  (generated while actually reading the resources) are NOT returned to the caller's
     *  out-list on a cached read. They are logged, and are reachable via
     *  getDelayedLogItems() on the returned config, but the out-list does not receive them.
     */
    public void testParseTimeItemsAreNotReturnedToCallerOnCachedRead()
    {
        CfgScenario s = CfgScenario.open( "errors" );
        try
        {
            List out = new ArrayList();
            Object mpc = s.asProvidedCached( new String[] { "/missing.properties", "/good.properties" }, out );

            List<String[]> outItems = s.renderLogItems( out );
            assertFalse( "the caller's out-list should not receive parse-time items:" + CfgScenario.describe( outItems ),
                         CfgScenario.hasLogItem( outItems, "FINE", "could not be found. Skipping." ) );

            List<String[]> onConfig = s.getDelayedLogItems( mpc );
            assertTrue( "but the config itself should carry them:" + CfgScenario.describe( onConfig ),
                        CfgScenario.hasLogItem( onConfig, "FINE", "could not be found. Skipping." ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** An uncached read, by contrast, appends parse-time items to the caller's list. */
    public void testUncachedReadReturnsParseTimeItemsToCaller()
    {
        CfgScenario s = CfgScenario.open( "errors" );
        try
        {
            List out = new ArrayList();
            s.asProvidedUncached( new String[] { "/missing.properties", "/good.properties" }, out );

            List<String[]> outItems = s.renderLogItems( out );
            assertTrue( "uncached reads should hand parse-time items back:" + CfgScenario.describe( outItems ),
                        CfgScenario.hasLogItem( outItems, "FINE", "could not be found. Skipping." ) );
        }
        finally
        { s.closeQuietly(); }
    }
}
