package com.mchange.v2.cfg;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 *  SealedSystemProperties: a one-time snapshot of System properties, so that configuration
 *  whose meaning must not change after startup cannot be rewritten afterward.
 *
 *  <p>Sealing is a one-shot static, once per JVM, and mill runs the whole suite in a single
 *  JVM (testParallelism = false). These tests therefore reset the seal reflectively around
 *  each case. That is deliberate and confined to the test: the production class offers no
 *  reset, and should not -- a seal that could be undone would not be a seal. Resetting here
 *  is also what keeps these cases from poisoning every later test class, since the first
 *  thing to consult configuration would otherwise fix the snapshot for the whole run.</p>
 */
public class SealedSystemPropertiesInternalJUnitTestCase extends TestCase
{
    private final static String PFX = "com.mchange.v2.cfg.junit.sealed";
    private final static String KEY = PFX + ".key";

    private Properties saved;

    @Override
    public void setUp() throws Exception
    {
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
        unseal();
    }

    @Override
    public void tearDown() throws Exception
    {
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                System.setProperty( k, saved.getProperty( k ) );
        unseal();
    }

    /** Restores the pre-sealed state; see the class comment for why this exists only here. */
    private static void unseal() throws Exception
    {
        Field f = SealedSystemProperties.class.getDeclaredField( "theProperties" );
        f.setAccessible( true );
        f.set( null, null );
    }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                doomed.add( k );
        for ( String k : doomed )
            System.clearProperty( k );
    }

    // ---------- when the seal happens ----------

    /** Asking must not answer by changing the answer. */
    public void testIsSealedDoesNotItselfSeal()
    {
        assertFalse( SealedSystemProperties.isSealed() );
        assertFalse( "Repeated interrogation must still not seal.", SealedSystemProperties.isSealed() );
    }

    /**
     *  get() is not a passive accessor: the first call fixes the snapshot. This is the
     *  lazy half of the design, and the reason any consumer can be the one that seals.
     */
    public void testGetSeals()
    {
        assertFalse( SealedSystemProperties.isSealed() );
        SealedSystemProperties.get();
        assertTrue( SealedSystemProperties.isSealed() );
    }

    /** seal() reports whether the call is what sealed, so a caller can tell it was too late. */
    public void testSealReportsWhetherItWasTheSealer()
    {
        assertTrue( "The first call seals and says so.", SealedSystemProperties.seal() );
        assertTrue( SealedSystemProperties.isSealed() );
        assertFalse( "A later call cannot re-seal, and says so.", SealedSystemProperties.seal() );
    }

    public void testSealAfterAnIncidentalGetReportsFalse()
    {
        SealedSystemProperties.get();

        assertFalse( "Something else already sealed; seal() must not claim otherwise.",
                     SealedSystemProperties.seal() );
    }

    // ---------- what the snapshot contains ----------

    public void testPropertiesSetBeforeSealingAreVisible()
    {
        System.setProperty( KEY, "before" );
        SealedSystemProperties.seal();

        assertEquals( "before", SealedSystemProperties.get().getProperty( KEY ) );
    }

    /** The point of the whole class: a later write cannot change what was sealed. */
    public void testPropertiesSetAfterSealingAreInvisible()
    {
        System.setProperty( KEY, "before" );
        SealedSystemProperties.seal();

        System.setProperty( KEY, "after" );

        assertEquals( "The snapshot must not follow the live value.",
                      "before", SealedSystemProperties.get().getProperty( KEY ) );
        assertEquals( "Precondition: the live property really did change.",
                      "after", System.getProperty( KEY ) );
    }

    /** and neither can a later removal. */
    public void testPropertiesClearedAfterSealingRemainVisible()
    {
        System.setProperty( KEY, "before" );
        SealedSystemProperties.seal();

        System.clearProperty( KEY );

        assertEquals( "before", SealedSystemProperties.get().getProperty( KEY ) );
        assertNull( System.getProperty( KEY ) );
    }

    /** A key created only after the seal is not in it at all. */
    public void testPropertiesCreatedAfterSealingAreAbsent()
    {
        SealedSystemProperties.seal();
        System.setProperty( KEY, "late" );

        assertNull( SealedSystemProperties.get().getProperty( KEY ) );
    }

    public void testGetReturnsTheSameSnapshotEveryTime()
    {
        PropertiesConfig first = SealedSystemProperties.get();

        System.setProperty( KEY, "churn" );

        assertSame( "There is exactly one snapshot per JVM.", first, SealedSystemProperties.get() );
    }

    // ---------- the MConfig facade ----------

    /** MConfig is where callers look first; its forwarders must mean the same thing. */
    public void testMConfigForwardersAgree()
    {
        assertFalse( MConfig.isSealedSystemProperties() );

        System.setProperty( KEY, "via-mconfig" );
        assertTrue( "MConfig.sealSystemProperties() must seal.", MConfig.sealSystemProperties() );

        assertTrue( MConfig.isSealedSystemProperties() );
        assertSame( SealedSystemProperties.get(), MConfig.getSealedSystemProperties() );
        assertEquals( "via-mconfig", MConfig.getSealedSystemProperties().getProperty( KEY ) );
        assertFalse( MConfig.sealSystemProperties() );
    }

    // ---------- the notional resource path ----------

    //  The snapshot used to report its resource path as "/", the same identifier any live read
    //  of System properties reports. That conflated two things that exist precisely because
    //  they can differ, and it made the snapshot indistinguishable from live System properties
    //  in every provenance report. It also meant combining the snapshot into a config that
    //  already read "/" named one resource path from two configs, which MConfig.combine
    //  explicitly declines to define.

    /** The identifier has to be something other than "/", or none of the rest follows. */
    public void testNotionalResourcePathIsDistinctFromSystemProperties()
    {
        assertFalse( "the snapshot must not share the live System properties identifier",
                     "/".equals( SealedSystemProperties.NOTIONAL_RESOURCE_PATH ) );
        assertFalse( "a leading slash would be routed to the classloader-resource source",
                     SealedSystemProperties.NOTIONAL_RESOURCE_PATH.startsWith( "/" ) );
    }

    public void testTheSnapshotReportsTheNotionalResourcePath()
    {
        assertEquals( Arrays.asList( SealedSystemProperties.NOTIONAL_RESOURCE_PATH ),
                      Arrays.asList( SealedSystemProperties.get().getPropertiesResourcePaths() ) );
    }

    /** It is a readable resource path, not merely a label, so it can appear in a path list. */
    public void testTheNotionalPathIsReadableAsAResourcePath()
    {
        System.setProperty( KEY, "sealed-value" );
        SealedSystemProperties.seal();

        MultiPropertiesConfig viaPath = MConfig.AsProvided.readUncachedResourceConfig(
            new String[] { SealedSystemProperties.NOTIONAL_RESOURCE_PATH } );

        assertEquals( "sealed-value", viaPath.getProperty( KEY ) );
    }

    /**
     *  THE REASON THE IDENTIFIER CHANGED. Read as resource paths, "/" and the notional path
     *  must be able to disagree -- "/" following the live value while the snapshot holds what
     *  it sealed. Sharing one identifier made this undetectable.
     */
    public void testTheNotionalPathAndSlashDivergeAfterSealing()
    {
        System.setProperty( KEY, "before" );
        SealedSystemProperties.seal();
        System.setProperty( KEY, "after" );

        String live   = MConfig.AsProvided.readUncachedResourceConfig( new String[] { "/" } ).getProperty( KEY );
        String sealed = MConfig.AsProvided.readUncachedResourceConfig(
                            new String[] { SealedSystemProperties.NOTIONAL_RESOURCE_PATH } ).getProperty( KEY );

        assertEquals( "a live read must follow the live value", "after",  live );
        assertEquals( "the snapshot must hold what it sealed",  "before", sealed );
    }

    /**
     *  AsProvided refuses vetoable identifiers up front, so this also records that the
     *  snapshot source cannot abort a read.
     */
    public void testTheNotionalPathIsNotVetoable()
    {
        MConfig.AsProvided.readUncachedResourceConfig(
            new String[] { SealedSystemProperties.NOTIONAL_RESOURCE_PATH } );
        // reaching here is the assertion: AsProvided would have thrown IllegalArgumentException
    }

    // ---------- the notional path inside a sequence of resource paths ----------

    //  Reading the path alone proves the source works. What the capping helpers actually rely
    //  on is that it behaves like any other element of a resource-path list: that it layers,
    //  that its POSITION decides who wins, that it merges rather than shadows, and that the
    //  provenance views see it. BasicMultiPropertiesConfig builds all of that from the path
    //  sequence, so these read through a sequence rather than past it.
    //
    //  SEQ_RESOURCE collides with the snapshot on this fixture's own key, so staging a conflict
    //  needs no write to a System property the fixture does not own and could not restore.

    private final static String SEQ_RESOURCE = "/com/mchange/v2/cfg/sealedseq.properties";
    private final static String ONLY_RESOURCE_KEY = PFX + ".onlyresource";

    /** A resource read, with the snapshot capped above it, exactly as the helpers arrange. */
    public void testTheNotionalPathLayersOverAnEarlierResource()
    {
        System.setProperty( KEY, "from-system" );
        SealedSystemProperties.seal();

        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedResourceConfig(
            new String[] { SEQ_RESOURCE, SEALED } );

        assertEquals( "the later path must win", "from-system", mpc.getProperty( KEY ) );
        assertEquals( Arrays.asList( SEQ_RESOURCE, SEALED ),
                      Arrays.asList( mpc.getPropertiesResourcePaths() ) );
    }

    /** ...and position is what decides it, not the kind of source. */
    public void testTheNotionalPathLosesToALaterResource()
    {
        System.setProperty( KEY, "from-system" );
        SealedSystemProperties.seal();

        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedResourceConfig(
            new String[] { SEALED, SEQ_RESOURCE } );

        assertEquals( "a path after the snapshot must still override it",
                      "from-resource", mpc.getProperty( KEY ) );
    }

    /** Merging, not shadowing: keys only the other path defines must survive alongside. */
    public void testTheNotionalPathMergesRatherThanShadowing()
    {
        System.setProperty( KEY, "from-system" );
        SealedSystemProperties.seal();

        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedResourceConfig(
            new String[] { SEQ_RESOURCE, SEALED } );

        assertEquals( "from-system", mpc.getProperty( KEY ) );
        assertEquals( "a key only the earlier path defines must survive",
                      "yes", mpc.getProperty( ONLY_RESOURCE_KEY ) );
    }

    /** The provenance views must see it as read, like any other source. */
    public void testTheNotionalPathIsReportedAsRead()
    {
        System.setProperty( KEY, "sealed-value" );
        SealedSystemProperties.seal();

        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedResourceConfig(
            new String[] { SEQ_RESOURCE, SEALED } );

        assertTrue( "wasRead must be true for the snapshot path", mpc.wasRead( SEALED ) );
        assertTrue( "and it must appear in getAllRead: " + mpc.getAllRead(),
                    mpc.getAllRead().contains( SEALED ) );
        assertFalse( mpc.wasNotFound( SEALED ) );
        assertFalse( mpc.wasVetoed( SEALED ) );
        assertFalse( mpc.wasFault( SEALED ) );
        assertEquals( "and its properties must be reachable by path",
                      "sealed-value", mpc.getPropertiesByResourcePath( SEALED ).getProperty( KEY ) );
    }

    /**
     *  The arrangement the helpers actually produce: a resource, then live System properties,
     *  then the snapshot. All three must contribute, and the snapshot must win where it and a
     *  live read disagree.
     */
    public void testAFullyCappedSequenceRanksResourceThenLiveThenSealed()
    {
        System.setProperty( KEY, "before" );
        SealedSystemProperties.seal();
        System.setProperty( KEY, "after" );

        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedResourceConfig(
            SealedSystemProperties.capCurrentThenSealed( new String[] { SEQ_RESOURCE } ) );

        assertEquals( Arrays.asList( SEQ_RESOURCE, "/", SEALED ),
                      Arrays.asList( mpc.getPropertiesResourcePaths() ) );
        assertEquals( "the snapshot caps the live read", "before", mpc.getProperty( KEY ) );
        assertEquals( "the live read is still present underneath", "after",
                      mpc.getPropertiesByResourcePath( "/" ).getProperty( KEY ) );
        assertEquals( "and the resource underneath both still contributes",
                      "yes", mpc.getPropertiesByResourcePath( SEQ_RESOURCE ).getProperty( ONLY_RESOURCE_KEY ) );
    }

    // ---------- capCurrentThenSealed(String[]) ----------

    private static List<String> capped( String... paths )
    { return Arrays.asList( SealedSystemProperties.capCurrentThenSealed( paths ) ); }

    private final static String SEALED = SealedSystemProperties.NOTIONAL_RESOURCE_PATH;

    /** Live System properties, then the snapshot above them, always last. */
    public void testCapAppendsSlashThenSealed()
    {
        assertEquals( Arrays.asList( "/a.properties", "/b.properties", "/", SEALED ),
                      capped( "/a.properties", "/b.properties" ) );
    }

    /**
     *  Both are stripped from wherever they appear first, rather than merely appended. Leaving
     *  an earlier "/" in place would leave System properties at a position something else
     *  could outrank, which is the whole thing this prevents.
     */
    public void testCapStripsEarlierOccurrencesRatherThanLeavingThemLow()
    {
        assertEquals( Arrays.asList( "/a.properties", "/b.properties", "/", SEALED ),
                      capped( "/", "/a.properties", SEALED, "/b.properties" ) );
    }

    /** Everything else keeps its relative order. */
    public void testCapPreservesTheOrderOfOtherPaths()
    {
        assertEquals( Arrays.asList( "/one", "/two", "/three", "/", SEALED ),
                      capped( "/one", "/two", "/three" ) );
    }

    public void testCapIsIdempotent()
    {
        String[] once  = SealedSystemProperties.capCurrentThenSealed( new String[] { "/a.properties" } );
        String[] twice = SealedSystemProperties.capCurrentThenSealed( once );
        assertEquals( Arrays.asList( once ), Arrays.asList( twice ) );
    }

    public void testCapOfNothingIsJustTheTwo()
    { assertEquals( Arrays.asList( "/", SEALED ), capped() ); }

    // ---------- capCurrentThenSealAndCombine(MultiPropertiesConfig[]) ----------

    private static MultiPropertiesConfig override( String path, String key, String value )
    {
        Properties p = new Properties();
        p.setProperty( key, value );
        return MultiPropertiesConfig.fromProperties( path, p );
    }

    /** The snapshot outranks a live read, so a post-seal write cannot win. */
    public void testCombineRanksSealedAboveLiveSystemProperties()
    {
        System.setProperty( KEY, "before" );
        SealedSystemProperties.seal();
        System.setProperty( KEY, "after" );

        MultiPropertiesConfig combined = SealedSystemProperties.capCurrentThenSealAndCombine(
            new MultiPropertiesConfig[] { override( "/some.properties", "unrelated", "x" ) } );

        assertEquals( "before", combined.getProperty( KEY ) );
    }

    /** And an override cannot outrank either of them. That is the point of capping. */
    public void testCombineRanksOverridesBelowSystemProperties()
    {
        System.setProperty( KEY, "from-system" );
        SealedSystemProperties.seal();

        MultiPropertiesConfig combined = SealedSystemProperties.capCurrentThenSealAndCombine(
            new MultiPropertiesConfig[] { override( "/some.properties", KEY, "from-override" ) } );

        assertEquals( "from-system", combined.getProperty( KEY ) );
    }

    /** Capping must not cost the override the keys nothing else claims. */
    public void testCombineKeepsOverrideValuesThatDoNotCollide()
    {
        SealedSystemProperties.seal();

        MultiPropertiesConfig combined = SealedSystemProperties.capCurrentThenSealAndCombine(
            new MultiPropertiesConfig[] { override( "/some.properties", KEY, "from-override" ) } );

        assertEquals( "from-override", combined.getProperty( KEY ) );
        assertTrue( "the override's own path should survive: " + Arrays.asList( combined.getPropertiesResourcePaths() ),
                    Arrays.asList( combined.getPropertiesResourcePaths() ).contains( "/some.properties" ) );
    }

    /**
     *  An override that names "/" or the notional path itself is normalized away, rather than
     *  being allowed to pose as System properties at a position of its choosing.
     */
    public void testCombineDiscardsOverrideContributionsAtTheCappedPaths()
    {
        System.setProperty( KEY, "from-system" );
        SealedSystemProperties.seal();

        MultiPropertiesConfig combined = SealedSystemProperties.capCurrentThenSealAndCombine(
            new MultiPropertiesConfig[] { override( "/", KEY, "posing-as-live" ),
                                          override( SEALED, KEY, "posing-as-sealed" ) } );

        assertEquals( "from-system", combined.getProperty( KEY ) );
    }

    /** The capped result always ends in live-then-sealed, whatever went in. */
    public void testCombineEndsInLiveThenSealed()
    {
        SealedSystemProperties.seal();

        String[] paths = SealedSystemProperties.capCurrentThenSealAndCombine(
            new MultiPropertiesConfig[] { override( "/some.properties", "k", "v" ) } ).getPropertiesResourcePaths();

        assertEquals( SEALED, paths[ paths.length - 1 ] );
        assertEquals( "/",    paths[ paths.length - 2 ] );
    }

    /**
     *  Diagnostics must survive the rebuild. capCurrentThenSealAndCombine re-keys each
     *  override's properties into fresh configs, which carry no log items of their own, so the
     *  originals' items have to be carried across explicitly -- and once per override, not
     *  once per resource path.
     */
    public void testCombineCarriesOverrideLogItemsAcrossExactlyOnce()
    {
        SealedSystemProperties.seal();

        List<DelayedLogItem> items = new ArrayList<DelayedLogItem>();
        MultiPropertiesConfig multiPath = MConfig.AsProvided.readUncachedResourceConfig(
            new String[0],
            new String[] { "/com/mchange/v2/cfg/junit/a.properties", "/mchange-commons.properties" },
            items );

        int before = multiPath.getDelayedLogItems().size();
        assertTrue( "precondition: the override reports log items of its own", before > 0 );
        assertTrue( "precondition: it reports more than one resource path",
                    multiPath.getPropertiesResourcePaths().length > 1 );

        MultiPropertiesConfig combined = SealedSystemProperties.capCurrentThenSealAndCombine(
            new MultiPropertiesConfig[] { multiPath } );

        int carried = 0;
        for ( DelayedLogItem item : combined.getDelayedLogItems() )
            if ( multiPath.getDelayedLogItems().contains( item ) )
                ++carried;

        assertEquals( "each of the override's items should appear exactly once, not once per path",
                      before, carried );
    }

    /**
     *  THE CASE THAT MOTIVATED CARRYING THEM. A wholly vetoed override reports NO resource
     *  paths at all, so harvesting its diagnostics per path harvests nothing -- and the
     *  AsProvidedIgnoreVetoed warning naming what it dropped, which is that facade's only
     *  safety feature, is lost in exactly the usage the manual recommends.
     */
    public void testCombineCarriesDiagnosticsFromAWhollyVetoedOverride() throws Exception
    {
        if ( !posixSupported() ) return;

        Path dir = Files.createTempDirectory( "mchange-cfg-cap-" );
        Path worldRead = dir.resolve( "worldread.properties" );
        try
        {
            Files.write( worldRead, "secret.key=from-worldread\n".getBytes( "8859_1" ) );
            Files.setPosixFilePermissions( worldRead, PosixFilePermissions.fromString( "rw-r--r--" ) );
            String vetoing = worldRead.toUri().toString() + "?permissions=useronly";

            SealedSystemProperties.seal();

            MultiPropertiesConfig vetoed = MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig(
                new String[0], new String[] { vetoing }, new ArrayList<DelayedLogItem>() );

            assertEquals( "precondition: a wholly vetoed read reports no resource paths",
                          0, vetoed.getPropertiesResourcePaths().length );
            assertTrue( "precondition: but it does report the veto",
                        vetoed.getAllVetoed().contains( vetoing ) );

            MultiPropertiesConfig combined = SealedSystemProperties.capCurrentThenSealAndCombine(
                new MultiPropertiesConfig[] { vetoed } );

            assertTrue( "the veto WARNING must survive capping, got: " + describe( combined.getDelayedLogItems() ),
                        hasWarningContaining( combined.getDelayedLogItems(), "has vetoed config" ) );
            assertTrue( "and the vetoed path must still be reported as vetoed: " + combined.getAllVetoed(),
                        combined.getAllVetoed().contains( vetoing ) );
        }
        finally
        {
            try { Files.deleteIfExists( worldRead ); Files.deleteIfExists( dir ); }
            catch ( IOException e ) { /* best effort */ }
        }
    }

    private boolean posixSupported()
    {
        try
        { return Files.getFileStore( Files.createTempDirectory( "mchange-posix-probe-" ) ).supportsFileAttributeView( "posix" ); }
        catch ( Exception e )
        { return false; }
    }

    private static boolean hasWarningContaining( List<DelayedLogItem> items, String fragment )
    {
        for ( DelayedLogItem item : items )
            if ( DelayedLogItem.Level.WARNING.equals( item.getLevel() )
                 && item.getText() != null && item.getText().contains( fragment ) )
                return true;
        return false;
    }

    private static String describe( List<DelayedLogItem> items )
    {
        StringBuilder sb = new StringBuilder();
        for ( DelayedLogItem item : items )
            sb.append( "\n    [" ).append( item.getLevel() ).append( "] " ).append( item.getText() );
        return sb.toString();
    }
}
