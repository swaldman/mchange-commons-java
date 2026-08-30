package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import com.mchange.v2.cfg.MConfig;
import com.mchange.v2.cfg.MultiPropertiesConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 *  What became of each identifier a read was given, rather than only what survived it.
 *
 *  <p>{@code getPropertiesResourcePaths()} reports the paths that yielded configuration; every
 *  other outcome used to leave no trace but a log item. That is a real gap for
 *  {@link MConfig.WithTraditionalDefaultSources}, which is defined to tolerate a veto: it drops
 *  the refusing source, warns, and returns a perfectly usable config. An application that cares
 *  whether its credentials file was refused had nothing to ask.</p>
 *
 *  <p>So a config now sorts every identifier into one of four buckets -- read, vetoed, not found,
 *  faulted -- and these tests fix that classification, the two properties the buckets have as a
 *  set, and the rule that resolves disagreement when configs are combined.</p>
 */
public final class ConfigHistoryJUnitTestCase extends TestCase
{
    private final static String[] NONE = new String[0];

    /** Present on the test classpath; other cfg tests read it too. */
    private final static String READS     = "/com/mchange/v2/cfg/junit/a.properties";
    private final static String NOT_FOUND = "/no-such-resource-for-history.properties";

    /** Neither absolute, nor a file: URL, nor hocon: -- rejected before anything is opened. */
    private final static String FAULTY    = "relative-malformed.properties";

    private final static String NEVER_NAMED = "/never-named-by-any-read.properties";

    private Path   dir;
    private Path   worldRead;
    private String vetoing;

    protected void setUp() throws Exception
    {
        dir       = Files.createTempDirectory( "mchange-cfg-history-" );
        worldRead = write( "worldread.properties", "secret.key=from-worldread\n", "rw-r--r--" );
        vetoing   = worldRead.toUri().toString() + "?permissions=useronly";
    }

    protected void tearDown() throws Exception
    { deleteQuietly( worldRead ); deleteQuietly( dir ); }

    private Path write( String name, String contents, String mode ) throws IOException
    {
        Path p = dir.resolve( name );
        Files.write( p, contents.getBytes( "8859_1" ) );
        if ( posixSupported() ) Files.setPosixFilePermissions( p, PosixFilePermissions.fromString( mode ) );
        return p;
    }

    private static void deleteQuietly( Path p )
    { try { if ( p != null ) Files.deleteIfExists( p ); } catch ( IOException e ) { /* best effort */ } }

    private boolean posixSupported()
    { return dir.getFileSystem().supportedFileAttributeViews().contains( "posix" ); }

    // ------------------------------------------------------------- the classification

    /**
     *  Read, absent and malformed land in three different buckets.
     *
     *  <p>Through AsProvided, which reads only what it is given -- the traditional facade would
     *  add the backstop paths and put entries in these buckets that the test never asked for.</p>
     */
    public void testEachOutcomeLandsInItsOwnBucket()
    {
        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedClassloaderResourceConfig(
            NONE, new String[] { READS, NOT_FOUND, FAULTY }, new ArrayList() );

        assertTrue(  "the readable resource should be read: "   + describe( mpc ), mpc.wasRead( READS ) );
        assertTrue(  "the absent resource should be notFound: " + describe( mpc ), mpc.wasNotFound( NOT_FOUND ) );
        assertTrue(  "the malformed identifier should fault: "  + describe( mpc ), mpc.wasFault( FAULTY ) );

        assertFalse( "a read path is not a failure", mpc.wasVetoed( READS ) || mpc.wasNotFound( READS ) || mpc.wasFault( READS ) );
        assertFalse( "an absent path was not read",  mpc.wasRead( NOT_FOUND ) );
        assertFalse( "a faulted path was not read",  mpc.wasRead( FAULTY ) );
    }

    /**
     *  The point of the whole thing: the traditional facade swallows a veto by design, so the
     *  only way to learn of one is to ask afterwards.
     */
    public void testAToleratedVetoIsStillVisibleAfterwards()
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig mpc = MConfig.WithTraditionalDefaultSources
            .readUncachedClassloaderResourceConfig( NONE, new String[] { READS, vetoing }, new ArrayList() );

        assertTrue( "the read must have succeeded despite the veto", mpc.wasRead( READS ) );
        assertTrue( "the tolerated veto must be visible: " + describe( mpc ), mpc.wasVetoed( vetoing ) );
        assertFalse( "a vetoed source contributes nothing", mpc.wasRead( vetoing ) );
        assertNull(  "...and none of its properties",       mpc.getProperty( "secret.key" ) );
    }

    // ------------------------------------------------------------- the buckets as a set

    /** No identifier may appear in two buckets, or "what happened to X" has two answers. */
    public void testTheBucketsAreDisjoint()
    {
        if ( !posixSupported() ) return;

        assertDisjoint( MConfig.AsProvided.readUncachedClassloaderResourceConfig(
                            NONE, new String[] { READS, NOT_FOUND, FAULTY }, new ArrayList() ) );
        assertDisjoint( MConfig.WithTraditionalDefaultSources.readUncachedClassloaderResourceConfig(
                            NONE, new String[] { READS, NOT_FOUND, FAULTY, vetoing }, new ArrayList() ) );
    }

    /**
     *  The buckets are exhaustive, which is what makes wasEncountered meaningful: every
     *  identifier the read was given is in exactly one, and anything else is in none.
     */
    public void testWasEncounteredDistinguishesAttemptedFromNeverNamed()
    {
        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedClassloaderResourceConfig(
            NONE, new String[] { READS, NOT_FOUND, FAULTY }, new ArrayList() );

        assertTrue( "a path that was read was encountered",     mpc.wasEncountered( READS ) );
        assertTrue( "a path that was absent was encountered",   mpc.wasEncountered( NOT_FOUND ) );
        assertTrue( "a path that faulted was encountered",      mpc.wasEncountered( FAULTY ) );

        assertFalse( "a path no read ever saw was not encountered", mpc.wasEncountered( NEVER_NAMED ) );
        assertFalse( mpc.wasRead( NEVER_NAMED ) );
        assertFalse( mpc.wasVetoed( NEVER_NAMED ) );
        assertFalse( mpc.wasNotFound( NEVER_NAMED ) );
        assertFalse( mpc.wasFault( NEVER_NAMED ) );
    }

    // ------------------------------------------------------------- immutability

    /**
     *  The four sets are a config's answer about its own past, so a caller must not be able to
     *  edit it. Handing back the live set let a caller rewrite history: {@code
     *  getAllVetoed().add(p)} made {@code wasVetoed(p)} start returning true.
     *
     *  <p>Every construction route is checked, because they did not agree: reads built through
     *  firstInit wrapped their sets, while everything built through the combining constructor --
     *  which is to say every MConfig.combine(..) result, and fromProperties(..) -- did not.</p>
     */
    public void testTheHistorySetsAreUnmodifiable()
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig fromRead = MConfig.WithTraditionalDefaultSources
            .readUncachedClassloaderResourceConfig( NONE, new String[] { READS, vetoing }, new ArrayList() );

        Properties props = new Properties();
        props.setProperty( "a", "b" );

        assertUnmodifiable( "a plain read",              fromRead );
        assertUnmodifiable( "MConfig.combine(..)",       MConfig.combine( new MultiPropertiesConfig[] { fromRead } ) );
        assertUnmodifiable( "a combine of a combine",
                            MConfig.combine( new MultiPropertiesConfig[] {
                                MConfig.combine( new MultiPropertiesConfig[] { fromRead } ) } ) );
        assertUnmodifiable( "fromProperties(..)",        MultiPropertiesConfig.fromProperties( "/notional", props ) );
    }

    /** Editing a returned set must not be able to change what the config says happened. */
    public void testMutatingAReturnedSetCannotRewriteHistory()
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig fromRead = MConfig.WithTraditionalDefaultSources
            .readUncachedClassloaderResourceConfig( NONE, new String[] { vetoing }, new ArrayList() );
        MultiPropertiesConfig mpc = MConfig.combine( new MultiPropertiesConfig[] { fromRead } );

        assertFalse( "precondition", mpc.wasVetoed( NEVER_NAMED ) );
        try
        { mpc.getAllVetoed().add( NEVER_NAMED ); }
        catch ( UnsupportedOperationException expected )
        { /* the point */ }
        assertFalse( "a caller must not be able to invent a veto", mpc.wasVetoed( NEVER_NAMED ) );
    }

    // ------------------------------------------------------------- combining

    /**
     *  When combined configs disagree about one identifier, a success outranks any failure: the
     *  configuration really was obtained, whatever some other source made of the same name.
     */
    public void testCombineLetsAReadOutrankAFailure()
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig vetoedIt = MConfig.WithTraditionalDefaultSources
            .readUncachedClassloaderResourceConfig( NONE, new String[] { vetoing }, new ArrayList() );
        assertTrue( "precondition", vetoedIt.wasVetoed( vetoing ) );

        Properties props = new Properties();
        props.setProperty( "x", "y" );
        MultiPropertiesConfig readIt = MultiPropertiesConfig.fromProperties( vetoing, props );

        MultiPropertiesConfig combined = MConfig.combine( new MultiPropertiesConfig[] { vetoedIt, readIt } );
        assertTrue(  "a successful read should win", combined.wasRead( vetoing ) );
        assertFalse( "...and clear the failure",     combined.wasVetoed( vetoing ) );
    }

    /**
     *  Among failures a veto outranks the rest. A veto is a refusal on security grounds and is
     *  the signal this API exists to surface, so it must not be masked by another source merely
     *  failing to find the same name. The cascade originally ran the other way, and reported
     *  "not found" for an identifier that had actually been refused.
     *
     *  <p>The ambiguity is arranged honestly: the same identifier is read once while the file is
     *  present and world-readable (vetoed), and again once it has been deleted (not found).</p>
     */
    public void testCombineLetsAVetoOutrankANotFound() throws Exception
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig vetoedIt = MConfig.WithTraditionalDefaultSources
            .readUncachedClassloaderResourceConfig( NONE, new String[] { vetoing }, new ArrayList() );
        assertTrue( "precondition: the file is present and insecure", vetoedIt.wasVetoed( vetoing ) );

        Files.delete( worldRead );

        MultiPropertiesConfig missedIt = MConfig.WithTraditionalDefaultSources
            .readUncachedClassloaderResourceConfig( NONE, new String[] { vetoing }, new ArrayList() );
        assertTrue( "precondition: the same identifier now finds nothing", missedIt.wasNotFound( vetoing ) );

        // asserted both ways round, so it is the priority rule rather than argument order
        MultiPropertiesConfig a = MConfig.combine( new MultiPropertiesConfig[] { missedIt, vetoedIt } );
        assertTrue(  "the veto must survive the combine", a.wasVetoed( vetoing ) );
        assertFalse( "and must not be reported as merely absent", a.wasNotFound( vetoing ) );

        MultiPropertiesConfig b = MConfig.combine( new MultiPropertiesConfig[] { vetoedIt, missedIt } );
        assertTrue(  "...in either order", b.wasVetoed( vetoing ) );
        assertFalse( b.wasNotFound( vetoing ) );
    }

    // ------------------------------------------------------------- helpers

    private static void assertDisjoint( MultiPropertiesConfig mpc )
    {
        String[] names = { "read", "vetoed", "notFound", "faulted" };
        Set[]    sets  = { mpc.getAllRead(), mpc.getAllVetoed(), mpc.getAllNotFound(), mpc.getAllFaults() };

        Map<String,String> seenIn = new TreeMap<String,String>();
        for ( int i = 0; i < names.length; ++i )
            for ( Object o : sets[i] )
            {
                String path = String.valueOf( o );
                String already = seenIn.get( path );
                if ( already != null )
                    fail( "'" + path + "' is in both the " + already + " and " + names[i] + " buckets" );
                seenIn.put( path, names[i] );
            }
        assertTrue( "nothing was classified at all, so this would pass vacuously", seenIn.size() > 0 );
    }

    @SuppressWarnings("unchecked")
    private static void assertUnmodifiable( String label, MultiPropertiesConfig mpc )
    {
        String[] names = { "getAllRead", "getAllVetoed", "getAllNotFound", "getAllFaults" };
        Set[]    sets  = { mpc.getAllRead(), mpc.getAllVetoed(), mpc.getAllNotFound(), mpc.getAllFaults() };

        for ( int i = 0; i < names.length; ++i )
        {
            try
            {
                sets[i].add( NEVER_NAMED );
                fail( label + ": " + names[i] + "() handed back a modifiable set" );
            }
            catch ( UnsupportedOperationException expected )
            { /* the point */ }
        }
    }

    private static String describe( MultiPropertiesConfig mpc )
    {
        return "\n    read     = " + mpc.getAllRead()
             + "\n    vetoed   = " + mpc.getAllVetoed()
             + "\n    notFound = " + mpc.getAllNotFound()
             + "\n    faulted  = " + mpc.getAllFaults();
    }
}
