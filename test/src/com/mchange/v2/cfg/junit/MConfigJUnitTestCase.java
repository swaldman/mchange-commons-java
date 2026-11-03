package com.mchange.v2.cfg.junit;

import junit.framework.*;
import com.mchange.v2.cfg.*;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/**
 *  The original cfg test case, retained and extended.
 *
 *  The testNoSystemConfig / testSystemShadows / testSystemShadowed trio predates the
 *  facade reorganization and still exercises the deprecated MConfig.readConfig shim,
 *  which is worth keeping as compatibility coverage. Each is paired with an equivalent
 *  written against the supported MConfig.AsProvided API.
 *
 *  These run on the ordinary test classpath rather than through CfgScenario, because
 *  they depend only on named resources, not on the presence or absence of the magic
 *  resource-path files.
 */
public final class MConfigJUnitTestCase extends TestCase
{
    final static String RP_A = "/com/mchange/v2/cfg/junit/a.properties";
    final static String RP_B = "/com/mchange/v2/cfg/junit/b.properties";

    // ------------------------------------------------ deprecated API (compat)

    @SuppressWarnings("deprecation")
    public void testNoSystemConfig()
    {
	MultiPropertiesConfig mpc = MConfig.readConfig(new String[] {RP_A, RP_B});
	assertTrue( "/b/home".equals( mpc.getProperty( "user.home" ) ) );
    }

    @SuppressWarnings("deprecation")
    public void testSystemShadows()
    {
	MultiPropertiesConfig mpc = MConfig.readConfig(new String[] {RP_A, RP_B, "/"});
	assertTrue( (! "/b/home".equals( mpc.getProperty( "user.home" ) ) ) && 
		    (! "/a/home".equals( mpc.getProperty( "user.home" ) ) ) );
    }

    @SuppressWarnings("deprecation")
    public void testSystemShadowed()
    {
	MultiPropertiesConfig mpc = MConfig.readConfig(new String[] {RP_A, "/", RP_B});
	assertTrue( "/b/home".equals( mpc.getProperty( "user.home" ) ) );
    }

    // ---------------------------------------------------- supported API

    public void testNoSystemConfigAsProvided()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readCachedResourceConfig( new String[] {RP_A, RP_B} );
        assertEquals( "/b/home", mpc.getProperty( "user.home" ) );
    }

    public void testSystemShadowsAsProvided()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readCachedResourceConfig( new String[] {RP_A, RP_B, "/"} );
        assertEquals( System.getProperty( "user.home" ), mpc.getProperty( "user.home" ) );
    }

    public void testSystemShadowedAsProvided()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readCachedResourceConfig( new String[] {RP_A, "/", RP_B} );
        assertEquals( "/b/home", mpc.getProperty( "user.home" ) );
    }

    /** AsProvided also offers uncached reads, as the class documentation promises. */
    public void testAsProvidedUncachedReadsWork()
    {
        MultiPropertiesConfig first =
            MConfig.AsProvided.readUncachedResourceConfig( new String[] {RP_A, RP_B} );
        MultiPropertiesConfig second =
            MConfig.AsProvided.readUncachedResourceConfig( new String[] {RP_A, RP_B} );

        assertEquals( "/b/home", first.getProperty( "user.home" ) );
        assertTrue( "uncached reads should not be shared", first != second );
    }

    // -------------------------------------- deprecated shims delegate correctly

    /** MConfig.readConfig is exactly MConfig.AsProvided.readCachedResourceConfig. */
    @SuppressWarnings("deprecation")
    public void testReadConfigDelegatesToAsProvidedCached()
    {
        String[] paths = new String[] { RP_A, RP_B };
        assertSame( MConfig.AsProvided.readCachedResourceConfig( paths ),
                    MConfig.readConfig( paths ) );
    }

    /** MConfig.readVmConfig() is exactly the traditional cached read. */
    @SuppressWarnings("deprecation")
    public void testReadVmConfigDelegatesToTraditionalCached()
    {
        assertSame( MConfig.WithTraditionalDefaultSources.readCachedResourceConfig(),
                    MConfig.readVmConfig() );
    }

    // ------------------------------------- single-path conveniences delegate correctly

    /*
     *  Each kind offers readCachedResourceConfig(String) / readUncachedResourceConfig(String)
     *  as one-liners over the String[] forms. Six near-identical bodies spread across three
     *  near-identical classes is exactly the shape that acquires a copy-paste error, and the
     *  error would be silent: delegating to the wrong sibling or the wrong kind still returns
     *  a plausible config. These pin the wiring rather than the reading.
     */

    /**
     *  A cached single-path read must land on the very cache entry the String[] form does.
     *
     *  <p>assertSame carries the weight here. A single-path form miswired to the <i>uncached</i>
     *  sibling would still return the right properties, and only identity catches it. The same
     *  assertion also catches delegation to another kind, since each kind keys its own cache
     *  entry (see VetoableConfigJUnitTestCase, "as-provided and vetoable must not share an
     *  entry").</p>
     */
    public void testSinglePathCachedIsTheArrayFormInEveryKind()
    {
        assertSame( "AsProvided",
                    MConfig.AsProvided.readCachedResourceConfig( new String[] { RP_A } ),
                    MConfig.AsProvided.readCachedResourceConfig( RP_A ) );

        try
        {
            assertSame( "AsProvidedVetoable",
                        MConfig.AsProvidedVetoable.readCachedResourceConfig( new String[] { RP_A } ),
                        MConfig.AsProvidedVetoable.readCachedResourceConfig( RP_A ) );
        }
        catch ( ConfigVetoedException e )
        { fail( "a plain classpath resource cannot veto: " + e ); }

        assertSame( "AsProvidedIgnoreVetoed",
                    MConfig.AsProvidedIgnoreVetoed.readCachedResourceConfig( new String[] { RP_A } ),
                    MConfig.AsProvidedIgnoreVetoed.readCachedResourceConfig( RP_A ) );
    }

    /**
     *  And an uncached single-path read must stay uncached -- the mirror of the test above,
     *  which is what makes the pair able to tell the two siblings apart.
     */
    public void testSinglePathUncachedIsTheArrayFormInEveryKind()
    {
        MultiPropertiesConfig ap1 = MConfig.AsProvided.readUncachedResourceConfig( RP_A );
        MultiPropertiesConfig ap2 = MConfig.AsProvided.readUncachedResourceConfig( RP_A );
        assertEquals( "/a/home", ap1.getProperty( "user.home" ) );
        assertTrue( "AsProvided uncached reads should not be shared", ap1 != ap2 );

        try
        {
            MultiPropertiesConfig v1 = MConfig.AsProvidedVetoable.readUncachedResourceConfig( RP_A );
            MultiPropertiesConfig v2 = MConfig.AsProvidedVetoable.readUncachedResourceConfig( RP_A );
            assertEquals( "/a/home", v1.getProperty( "user.home" ) );
            assertTrue( "AsProvidedVetoable uncached reads should not be shared", v1 != v2 );
        }
        catch ( ConfigVetoedException e )
        { fail( "a plain classpath resource cannot veto: " + e ); }

        MultiPropertiesConfig iv1 = MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( RP_A );
        MultiPropertiesConfig iv2 = MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( RP_A );
        assertEquals( "/a/home", iv1.getProperty( "user.home" ) );
        assertTrue( "AsProvidedIgnoreVetoed uncached reads should not be shared", iv1 != iv2 );
    }

    /**
     *  The three cached single-path reads of one path are three different configs, because kind
     *  is part of the cache key. If any class's convenience delegated into a neighbor -- the
     *  likeliest copy-paste slip, the classes being so alike -- two of these would collapse.
     */
    public void testSinglePathCachedDoesNotCrossKinds() throws ConfigVetoedException
    {
        // these need identity rather than equals(..), so compare pairwise
        MultiPropertiesConfig ap = MConfig.AsProvided.readCachedResourceConfig( RP_A );
        MultiPropertiesConfig v  = MConfig.AsProvidedVetoable.readCachedResourceConfig( RP_A );
        MultiPropertiesConfig iv = MConfig.AsProvidedIgnoreVetoed.readCachedResourceConfig( RP_A );

        assertTrue( "AsProvided and AsProvidedVetoable must not share a cache entry",     ap != v );
        assertTrue( "AsProvided and AsProvidedIgnoreVetoed must not share a cache entry", ap != iv );
        assertTrue( "AsProvidedVetoable and AsProvidedIgnoreVetoed must not share one",   v  != iv );

        // every one of them still read the resource
        assertEquals( "/a/home", ap.getProperty( "user.home" ) );
        assertEquals( "/a/home", v.getProperty( "user.home" ) );
        assertEquals( "/a/home", iv.getProperty( "user.home" ) );
    }

    /*
     *  A file: identifier is vetoABLE whatever it points at -- vetoability is a property of the
     *  source type, not of the file (VetoableConfigJUnitTestCase,
     *  testVetoabilityIsAPropertyOfTheSourceNotTheIdentifier). So a file: URL naming a file that
     *  does not exist separates the kinds without needing POSIX permissions or a temp fixture:
     *  AsProvided refuses the identifier before reading anything, while the other two accept it
     *  and find mere absence, which is not a veto.
     */
    private final static String ABSENT_FILE_URL =
        new File( System.getProperty( "java.io.tmpdir" ),
                  "mchange-commons-absent-" + MConfigJUnitTestCase.class.getName() + ".properties" )
        .toURI().toString();

    /**
     *  AsProvided's refusal of vetoable identifiers has to survive the new entry points. Were
     *  the convenience to reach readForKind directly, or to route through another kind, this
     *  would quietly start accepting file: sources in the facade documented not to take them.
     */
    public void testSinglePathAsProvidedStillRefusesAVetoableIdentifier()
    {
        try
        {
            MConfig.AsProvided.readCachedResourceConfig( ABSENT_FILE_URL );
            fail( "AsProvided.readCachedResourceConfig(String) should refuse a file: identifier" );
        }
        catch ( IllegalArgumentException expected )
        { /* requireNoVetoableConfig is in the path, as it must be */ }

        try
        {
            MConfig.AsProvided.readUncachedResourceConfig( ABSENT_FILE_URL );
            fail( "AsProvided.readUncachedResourceConfig(String) should refuse a file: identifier" );
        }
        catch ( IllegalArgumentException expected )
        { /* and in this one too */ }
    }

    /**
     *  The contrast, and the other half of the discriminator: the two kinds that do accept
     *  vetoable identifiers must still accept them through the single-path forms. If either
     *  delegated into AsProvided, the IllegalArgumentException above would surface here.
     */
    public void testSinglePathVetoableKindsStillAcceptAVetoableIdentifier()
    {
        try
        {
            assertNotNull( MConfig.AsProvidedVetoable.readCachedResourceConfig( ABSENT_FILE_URL ) );
            assertNotNull( MConfig.AsProvidedVetoable.readUncachedResourceConfig( ABSENT_FILE_URL ) );
        }
        catch ( ConfigVetoedException e )
        { fail( "a missing file is absence, not a veto: " + e ); }

        assertNotNull( MConfig.AsProvidedIgnoreVetoed.readCachedResourceConfig( ABSENT_FILE_URL ) );
        assertNotNull( MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( ABSENT_FILE_URL ) );
    }

    // ------------------------------------------ fromProperties on the facade

    private static Properties props( String... kvs )
    {
        Properties p = new Properties();
        for ( int i = 0; i < kvs.length; i += 2 )
            p.setProperty( kvs[i], kvs[i+1] );
        return p;
    }

    /**
     *  MConfig.fromProperties(String,Properties) is MultiPropertiesConfig's, and names the path
     *  it was given as the config's sole resource path. Argument order is the thing worth
     *  pinning: swap the two and nothing fails to compile, because only one of them is a String.
     */
    public void testFacadeFromPropertiesNamesTheGivenPath()
    {
        MultiPropertiesConfig mpc = MConfig.fromProperties( "/notional", props( "k", "v" ) );

        assertEquals( Arrays.asList( "/notional" ),
                      Arrays.asList( mpc.getPropertiesResourcePaths() ) );
        assertEquals( "v", mpc.getProperty( "k" ) );
        assertEquals( "v", mpc.getPropertiesByResourcePath( "/notional" ).getProperty( "k" ) );
    }

    /**
     *  The no-path form allocates a fresh notional path per call, which is what its javadoc
     *  promises and what keeps two such configs from colliding under combine(..) -- the case
     *  combine declines to define, since a resource path holds exactly one precedence.
     */
    public void testFacadeFromPropertiesAllocatesADistinctPathPerCall()
    {
        Properties shared = props( "k", "v" );

        String firstPath  = MConfig.fromProperties( shared ).getPropertiesResourcePaths()[0];
        String secondPath = MConfig.fromProperties( shared ).getPropertiesResourcePaths()[0];

        assertFalse( "one Properties, read twice, must still yield two distinct notional paths",
                     firstPath.equals( secondPath ) );

        MultiPropertiesConfig combined = MConfig.combine( new MultiPropertiesConfig[] {
            MConfig.fromProperties( props( "k", "first" ) ),
            MConfig.fromProperties( props( "k", "second" ) )
        } );
        assertEquals( "distinct paths, so both elements are represented",
                      2, combined.getPropertiesResourcePaths().length );
        assertEquals( "and the later one wins by position", "second", combined.getProperty( "k" ) );
    }

    /**
     *  The Properties are copied on the way in, so a caller that keeps and mutates its own
     *  Properties cannot reach inside a live configuration.
     *
     *  <p>This is the invariant BasicMultiPropertiesConfig is built around, and that
     *  getPropertiesByPrefix once broke by handing out its internal Properties. The facade
     *  javadoc now states the guarantee, so something had better hold it: nothing else in the
     *  suite covers the copying of a caller-supplied map.</p>
     *
     *  <p>It has to be asserted through getPropertiesByResourcePath. getProperty and
     *  getPropertiesByPrefix read propsByKey and propsByPrefix, separate indexes built once at
     *  construction, so they cannot reflect a later mutation whether the map was copied or not
     *  &mdash; a test written against them passes vacuously. getPropertiesByResourcePath is the
     *  one accessor that reaches the stored Properties, returning a clone of whatever object the
     *  config actually holds.</p>
     *
     *  <p>Verified by mutation, which also turned up why this needed writing: the inbound copy
     *  is implemented <i>twice</i> on this path -- once where fromProperties clones the caller's
     *  map, and again where the six-argument constructor deep-copies every Properties it is
     *  given -- and either one alone is sufficient, so removing just one changes nothing
     *  observable. Only removing both fails this test. The redundancy is harmless, but it means
     *  neither line can be shown to be load-bearing on its own, and a refactor that removed one
     *  on the grounds that the other covers it would leave the guarantee resting on a single
     *  line with no test naming it. This test names it.</p>
     */
    public void testFacadeFromPropertiesCopiesTheCallerProperties()
    {
        Properties mutable = props( "k", "original" );

        MultiPropertiesConfig withPath = MConfig.fromProperties( "/notional", mutable );
        MultiPropertiesConfig noPath   = MConfig.fromProperties( mutable );
        String noPathPath = noPath.getPropertiesResourcePaths()[0];

        mutable.setProperty( "k", "mutated" );
        mutable.setProperty( "added.after", "yes" );

        assertEquals( "a later mutation must not reach the stored Properties",
                      "original", withPath.getPropertiesByResourcePath( "/notional" ).getProperty( "k" ) );
        assertEquals( "...nor those of the no-path form",
                      "original", noPath.getPropertiesByResourcePath( noPathPath ).getProperty( "k" ) );
        assertNull( "nor may a later addition appear among them",
                    withPath.getPropertiesByResourcePath( "/notional" ).getProperty( "added.after" ) );
        assertNull( "nor in the no-path form",
                    noPath.getPropertiesByResourcePath( noPathPath ).getProperty( "added.after" ) );

        // and the indexes, which were built before the mutation, agree
        assertEquals( "original", withPath.getProperty( "k" ) );
        assertNull( withPath.getProperty( "added.after" ) );
    }

    /**
     *  The other half of the same guarantee: what comes back out is a copy too, so a caller
     *  cannot mutate a live configuration through a Properties it was handed.
     */
    public void testFacadeFromPropertiesDoesNotHandOutItsStoredProperties()
    {
        MultiPropertiesConfig mpc = MConfig.fromProperties( "/notional", props( "k", "original" ) );

        Properties handedOut = mpc.getPropertiesByResourcePath( "/notional" );
        handedOut.setProperty( "k", "mutated" );
        handedOut.setProperty( "added.by.caller", "yes" );

        assertEquals( "mutating what we were handed must not reach the config",
                      "original", mpc.getPropertiesByResourcePath( "/notional" ).getProperty( "k" ) );
        assertNull( mpc.getPropertiesByResourcePath( "/notional" ).getProperty( "added.by.caller" ) );
        assertEquals( "original", mpc.getProperty( "k" ) );
    }
}
