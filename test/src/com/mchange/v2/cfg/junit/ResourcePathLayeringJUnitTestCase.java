package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 *  How a list of resource paths is condensed and layered: later sources override
 *  earlier ones, duplicates keep their highest-preference position, and "/" -- System
 *  properties -- participates positionally like any other source.
 */
public final class ResourcePathLayeringJUnitTestCase extends TestCase
{
    private final static String A = "/a.properties";
    private final static String B = "/b.properties";
    private final static String C = "/c.properties";

    private final static String SYS_A = "/sysprop-a.properties"; // user.home=/a/home
    private final static String SYS_B = "/sysprop-b.properties"; // user.home=/b/home

    private static List<String> readPaths( CfgScenario s, Object mpc )
    { return Arrays.asList( s.getPropertiesResourcePaths( mpc ) ); }

    public void testLastPathWins()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            assertEquals( "from-b", s.getProperty( s.asProvidedCached( new String[] { A, B } ), "shared" ) );
            assertEquals( "from-a", s.getProperty( s.asProvidedCached( new String[] { B, A } ), "shared" ) );
            assertEquals( "from-c", s.getProperty( s.asProvidedCached( new String[] { A, B, C } ), "shared" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Non-conflicting keys from every source are all visible. */
    public void testAllSourcesContributeNonConflictingKeys()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object mpc = s.asProvidedCached( new String[] { A, B, C } );
            assertEquals( "yes", s.getProperty( mpc, "only.a" ) );
            assertEquals( "yes", s.getProperty( mpc, "only.b" ) );
            assertEquals( "yes", s.getProperty( mpc, "only.c" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  A repeated path is deduplicated to its LATEST position, giving it maximum
     *  preference. So [A, B, A] resolves to [B, A] and A wins, not B.
     */
    public void testDuplicatePathKeepsItsLatestPosition()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object mpc = s.asProvidedCached( new String[] { A, B, A } );

            assertEquals( "duplicate should collapse to its last occurrence",
                          Arrays.asList( B, A ), readPaths( s, mpc ) );
            assertEquals( "from-a", s.getProperty( mpc, "shared" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** A path named in both defaults and preempts keeps the preempting slot. */
    public void testPathInBothDefaultsAndPreemptsKeepsPreemptingPosition()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object mpc = s.asProvidedCached( new String[] { A, B }, new String[] { A }, null );

            assertEquals( Arrays.asList( B, A ), readPaths( s, mpc ) );
            assertEquals( "from-a", s.getProperty( mpc, "shared" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** "/" is System properties, and does not shadow sources that come after it. */
    public void testSystemPropertiesAreShadowedByLaterSources()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object mpc = s.asProvidedCached( new String[] { SYS_A, "/", SYS_B } );
            assertEquals( "/b/home", s.getProperty( mpc, "user.home" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** "/" placed last shadows the resource-supplied values. */
    public void testSystemPropertiesShadowEarlierSources()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object mpc = s.asProvidedCached( new String[] { SYS_A, SYS_B, "/" } );
            assertEquals( "System properties should win when listed last",
                          System.getProperty( "user.home" ), s.getProperty( mpc, "user.home" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** With no "/" in the list, System properties are not consulted at all. */
    public void testSystemPropertiesAbsentWhenNotListed()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object mpc = s.asProvidedCached( new String[] { SYS_A, SYS_B } );
            assertEquals( "/b/home", s.getProperty( mpc, "user.home" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** null default/preempting arrays are normalized to empty, not dereferenced. */
    public void testNullPathArraysAreTreatedAsEmpty()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object justPreempts = s.asProvidedCached( null, new String[] { A }, null );
            assertEquals( "from-a", s.getProperty( justPreempts, "shared" ) );

            Object justDefaults = s.asProvidedCached( new String[] { B }, null, null );
            assertEquals( "from-b", s.getProperty( justDefaults, "shared" ) );

            Object neither = s.asProvidedCached( (String[]) null, (String[]) null, null );
            assertEquals( 0, readPaths( s, neither ).size() );
        }
        finally
        { s.closeQuietly(); }
    }

    /** An empty path list yields a usable, empty config. */
    public void testEmptyPathListYieldsEmptyConfig()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object mpc = s.asProvidedCached( new String[0] );
            assertEquals( 0, readPaths( s, mpc ).size() );
            assertNull( s.getProperty( mpc, "shared" ) );
            assertEquals( 0, s.getPropertiesByPrefix( mpc, "" ).size() );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Uncached reads layer identically to cached ones. */
    public void testUncachedReadsLayerIdentically()
    {
        CfgScenario s = CfgScenario.open( "layering" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { A, B, A } );
            assertEquals( Arrays.asList( B, A ), readPaths( s, mpc ) );
            assertEquals( "from-a", s.getProperty( mpc, "shared" ) );
        }
        finally
        { s.closeQuietly(); }
    }
}
