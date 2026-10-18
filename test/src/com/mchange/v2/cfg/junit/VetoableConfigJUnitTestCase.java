package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import com.mchange.v2.cfg.ConfigVetoedException;
import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.MConfig;
import com.mchange.v2.cfg.MultiPropertiesConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  What a veto MEANS, which differs by facade. This is the point of the VetoableConfig design:
 *  a config source that can abort a read is opt-in and visible in the type system, so end-user
 *  configuration cannot poison an application that never agreed to handle it.
 *
 *    AsProvidedVetoable  declares throws ConfigVetoedException. A veto aborts the read.
 *    AsProvided          refuses vetoable identifiers up front, with IllegalArgumentException.
 *                        Its paths are all caller-supplied, so naming one is a programmer error.
 *    Traditional         warns and ignores the vetoing source, keeping everything else. Its paths
 *                        can come from resource-path text files the application never wrote, so a
 *                        veto there is an end-user choice and must degrade rather than abort.
 *    AsProvidedIgnoreVetoed
 *                        warns and reads on, for an application that supplies vetoable paths
 *                        deliberately and would rather lose configuration than fail. It ignores
 *                        vetoes that OCCUR, not sources that COULD veto: a file: source that
 *                        loads cleanly is honored in full. Same paths as AsProvided, same veto
 *                        handling as Traditional.
 *
 *  The fixture is a world-readable file asked to be user-only, which is the simplest thing that
 *  reliably vetoes. FileUrlConfigJUnitTestCase covers WHY that source vetoes; this covers what
 *  each facade then does about it.
 */
public final class VetoableConfigJUnitTestCase extends TestCase
{
    private Path   dir;
    private Path   worldRead;   // 0644, asked to be useronly -> vetoes
    private Path   worldRead2;  // a second one, so a read can veto twice
    private Path   plain;       // ordinary, loads fine
    private String vetoing;
    private String vetoingToo;
    private String fine;

    private final static String[] NONE = new String[0];

    /** The sentence appended only to the veto that is actually going to be thrown. */
    private final static String PROMISE = "An Exception will be thrown.";

    @Override
    protected void setUp() throws Exception
    {
        dir        = Files.createTempDirectory( "mchange-cfg-veto-" );
        worldRead  = write( "worldread.properties",  "secret.key=from-worldread\n",  "rw-r--r--" );
        worldRead2 = write( "worldread2.properties", "secret.key=from-worldread2\n", "rw-r--r--" );
        plain      = write( "plain.properties",      "plain.key=from-plain\n",       "rw-------" );
        vetoing    = worldRead.toUri().toString()  + "?permissions=useronly";
        vetoingToo = worldRead2.toUri().toString() + "?permissions=useronly";
        fine       = plain.toUri().toString();
    }

    @Override
    protected void tearDown() throws Exception
    { deleteQuietly( worldRead ); deleteQuietly( worldRead2 ); deleteQuietly( plain ); deleteQuietly( dir ); }

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

    @SuppressWarnings("unchecked")
    private static boolean hasWarningContaining( MultiPropertiesConfig mpc, String fragment )
    {
        List<DelayedLogItem> items = mpc.getDelayedLogItems();
        for ( DelayedLogItem item : items )
            if ( DelayedLogItem.Level.WARNING.equals( item.getLevel() )
                 && item.getText() != null && item.getText().contains( fragment ) )
                return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    private static String describe( MultiPropertiesConfig mpc )
    {
        StringBuilder sb = new StringBuilder();
        List<DelayedLogItem> items = mpc.getDelayedLogItems();
        for ( DelayedLogItem item : items )
            sb.append( "\n    [" ).append( item.getLevel() ).append( "] " ).append( item.getText() );
        return sb.toString();
    }

    // ------------------------------------------- helpers for a read that THREW

    // A read that vetoes returns no config, so its report has to be read off the list the
    // caller passed in rather than off a MultiPropertiesConfig.

    private static List<String> vetoReportsIn( List items )
    {
        List<String> out = new ArrayList<String>();
        for ( Object o : items )
        {
            DelayedLogItem item = (DelayedLogItem) o;
            if ( DelayedLogItem.Level.WARNING.equals( item.getLevel() )
                 && item.getText() != null && item.getText().contains( "has vetoed config" ) )
                out.add( item.getText() );
        }
        return out;
    }

    private static String describe( List<String> reports )
    {
        StringBuilder sb = new StringBuilder();
        for ( String r : reports ) sb.append( "\n    " ).append( r );
        return sb.length() == 0 ? "(no veto reports)" : sb.toString();
    }

    /** Reads two vetoing identifiers in the given order and returns the veto that escaped. */
    private ConfigVetoedException readBothExpectingAVeto( String firstPath, String secondPath, List itemsOut )
    {
        try
        {
            MConfig.AsProvidedVetoable.readUncachedResourceConfig(
                NONE, new String[] { firstPath, secondPath }, itemsOut );
            fail( "expected a ConfigVetoedException from a read with two vetoing identifiers" );
            return null; // unreachable
        }
        catch ( ConfigVetoedException expected )
        { return expected; }
    }

    // =========================================== which veto, when there are several

    /**
     *  The FIRST veto encountered is the one thrown.
     *
     *  <p>Resource paths are ordered earliest-precedence first, so the first veto is the earliest
     *  failure in the read -- which is what a fail-fast reader reports and what someone reading
     *  the stack trace will expect. The loop used to keep overwriting and throw the last one.</p>
     *
     *  <p>Asserted in both orders with two interchangeable vetoing sources, so it pins the
     *  position rather than something incidental about either file.</p>
     */
    public void testTheFirstVetoEncounteredIsTheOneThrown()
    {
        if ( !posixSupported() ) return;

        ConfigVetoedException e1 = readBothExpectingAVeto( vetoing, vetoingToo, new ArrayList() );
        assertEquals( "the veto from the first identifier should have been thrown",
                      vetoing, e1.getIdentifier() );

        ConfigVetoedException e2 = readBothExpectingAVeto( vetoingToo, vetoing, new ArrayList() );
        assertEquals( "...and with the order reversed, the other one",
                      vetoingToo, e2.getIdentifier() );
    }

    /**
     *  Throwing one veto must not stop the others being reported. Only one exception can escape,
     *  so the log is the only place the remaining vetoes are ever mentioned -- and breaking out
     *  of the loop at the first one would silently lose them.
     */
    public void testEveryVetoIsReportedNotJustTheThrownOne()
    {
        if ( !posixSupported() ) return;

        List items = new ArrayList();
        readBothExpectingAVeto( vetoing, vetoingToo, items );

        List<String> reports = vetoReportsIn( items );
        assertEquals( "both vetoes should be reported, got:" + describe( reports ), 2, reports.size() );

        boolean sawFirst = false, sawSecond = false;
        for ( String r : reports )
        {
            if ( r.contains( vetoing ) )    sawFirst  = true;
            if ( r.contains( vetoingToo ) ) sawSecond = true;
        }
        assertTrue( "the thrown veto should be named, got:"     + describe( reports ), sawFirst );
        assertTrue( "the un-thrown veto should be named too, got:" + describe( reports ), sawSecond );
    }

    /**
     *  Only the thrown veto says an exception is coming. Every veto used to carry that sentence,
     *  so a read with several of them promised several exceptions and delivered one.
     */
    public void testOnlyTheThrownVetoPromisesAnException()
    {
        if ( !posixSupported() ) return;

        List items = new ArrayList();
        readBothExpectingAVeto( vetoing, vetoingToo, items );

        List<String> reports = vetoReportsIn( items );
        String promising = null;
        int    count     = 0;
        for ( String r : reports )
            if ( r.contains( PROMISE ) ) { ++count; promising = r; }

        assertEquals( "exactly one veto report should promise the exception, got:" + describe( reports ),
                      1, count );
        assertTrue( "the promise belongs on the veto that is actually thrown, got:\n    " + promising,
                    promising.contains( vetoing ) );
    }

    /** With a single veto the report is unchanged: it is the first, so it still promises the throw. */
    public void testASingleVetoStillPromisesTheException()
    {
        if ( !posixSupported() ) return;

        List items = new ArrayList();
        try
        {
            MConfig.AsProvidedVetoable.readUncachedResourceConfig( NONE, new String[] { vetoing }, items );
            fail( "expected a ConfigVetoedException" );
        }
        catch ( ConfigVetoedException expected )
        { /* expected */ }

        List<String> reports = vetoReportsIn( items );
        assertEquals( "one identifier, one veto report, got:" + describe( reports ), 1, reports.size() );
        assertTrue( "a lone veto is the first veto, so it promises the throw, got:" + describe( reports ),
                    reports.get( 0 ).contains( PROMISE ) );
    }

    // ==================================================== AsProvidedVetoable

    /** The opt-in facade: a veto aborts the read, as a checked exception. */
    public void testVetoableFacadeThrows()
    {
        if ( !posixSupported() ) return;
        try
        {
            MConfig.AsProvidedVetoable.readUncachedResourceConfig( NONE, new String[] { vetoing }, new ArrayList() );
            fail( "expected a ConfigVetoedException" );
        }
        catch ( ConfigVetoedException expected )
        { assertEquals( vetoing, expected.getIdentifier() ); }
    }

    /** Non-vetoing paths read normally through the vetoable facade. */
    public void testVetoableFacadeReadsNormallyWhenNothingVetoes() throws Exception
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvidedVetoable.readUncachedResourceConfig( NONE, new String[] { fine }, new ArrayList() );
        assertEquals( "from-plain", mpc.getProperty( "plain.key" ) );
    }

    /** The veto also survives the cached path with its own type intact. */
    public void testVetoableFacadeThrowsFromCachedReadsToo()
    {
        if ( !posixSupported() ) return;
        try
        {
            MConfig.AsProvidedVetoable.readCachedResourceConfig( new String[] { vetoing } );
            fail( "expected a ConfigVetoedException from the cached path" );
        }
        catch ( ConfigVetoedException expected )
        { /* not wrapped in RuntimeException(CachedStoreException(..)) */ }
    }

    // =========================================================== AsProvided

    /**
     *  The non-vetoable facade refuses vetoable identifiers before reading anything. Every path
     *  it sees was named by the caller in code, so this is a programmer error, and the message
     *  names both the offending identifiers and the facade to use instead.
     */
    public void testAsProvidedRefusesVetoableIdentifiers()
    {
        try
        {
            MConfig.AsProvided.readUncachedResourceConfig( NONE, new String[] { vetoing }, new ArrayList() );
            fail( "expected IllegalArgumentException" );
        }
        catch ( IllegalArgumentException expected )
        {
            String msg = expected.getMessage();
            assertTrue( "should name the offending identifier, was: " + msg, msg.contains( vetoing ) );
            assertTrue( "should name the facade to use instead, was: " + msg,
                        msg.contains( "AsProvidedVetoable" ) );
        }
    }

    /** It refuses on the strength of the identifier alone -- the file need not even be readable. */
    public void testAsProvidedRefusesEvenAnAbsentVetoableFile()
    {
        String absent = dir.resolve( "not-there.properties" ).toUri().toString() + "?required=true";
        try
        {
            MConfig.AsProvided.readUncachedResourceConfig( NONE, new String[] { absent }, new ArrayList() );
            fail( "expected IllegalArgumentException" );
        }
        catch ( IllegalArgumentException expected )
        { assertTrue( expected.getMessage().contains( "AsProvidedVetoable" ) ); }
    }

    /** The refusal is up front: a good path listed alongside a vetoable one is not read either. */
    public void testAsProvidedRefusalIsAllOrNothing()
    {
        try
        {
            MConfig.AsProvided.readUncachedResourceConfig( NONE, new String[] { fine, vetoing }, new ArrayList() );
            fail( "expected IllegalArgumentException" );
        }
        catch ( IllegalArgumentException expected )
        { /* expected */ }
    }

    /**
     *  Vetoability is a property of the SOURCE CLASS, not of the particular identifier: a file:
     *  URL is vetoable because FileUrlPropertiesConfigSource implements VetoableConfig, whether
     *  or not that identifier carries any option that could actually veto.
     *
     *  So AsProvided refuses plain file: URLs too. That is a real restriction -- reading an
     *  ordinary properties file off disk requires the vetoable facade -- but it is what makes
     *  the guarantee checkable without reading the file first, and it keeps the refusal
     *  decidable from the identifier alone.
     */
    public void testVetoabilityIsAPropertyOfTheSourceNotTheIdentifier()
    {
        try
        {
            // no query string at all: nothing here could possibly veto
            MConfig.AsProvided.readUncachedResourceConfig( NONE, new String[] { fine }, new ArrayList() );
            fail( "expected IllegalArgumentException even for an option-free file: URL" );
        }
        catch ( IllegalArgumentException expected )
        { assertTrue( expected.getMessage().contains( "AsProvidedVetoable" ) ); }
    }

    /** Non-vetoable identifiers pass through untouched. */
    public void testAsProvidedAcceptsNonVetoableIdentifiers()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readUncachedResourceConfig( NONE, new String[] { "/" }, new ArrayList() );
        assertEquals( System.getProperty( "user.home" ), mpc.getProperty( "user.home" ) );
    }

    /** Null path arrays are tolerated -- the vetoable check runs before nulls are normalized. */
    public void testAsProvidedToleratesNullPathArrays()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readUncachedResourceConfig( null, null, new ArrayList() );
        assertEquals( 0, mpc.getPropertiesResourcePaths().length );
    }

    // ========================================================== Traditional

    /**
     *  The traditional facade degrades. Its resource paths may come from
     *  /mchange-config-resource-paths.txt and friends, which the application does not control,
     *  so a veto must not be able to abort it. The vetoing source is dropped with a WARNING and
     *  everything else still loads.
     */
    public void testTraditionalWarnsAndIgnoresTheVetoingSource()
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig mpc = MConfig.WithTraditionalDefaultSources
            .readUncachedResourceConfig( new String[] { fine, vetoing }, NONE, new ArrayList() );

        assertEquals( "the non-vetoing source must still load", "from-plain", mpc.getProperty( "plain.key" ) );
        assertNull( "the vetoing source must contribute nothing", mpc.getProperty( "secret.key" ) );
        assertFalse( "the vetoing path should be dropped",
                     Arrays.asList( mpc.getPropertiesResourcePaths() ).contains( vetoing ) );
        assertTrue( "expected a WARNING saying the config was ignored, got:" + describe( mpc ),
                    hasWarningContaining( mpc, "ignored" ) );
    }

    /** Order does not matter -- a veto in the first position loses nothing that follows it. */
    public void testTraditionalIgnoresVetoWhereverItAppears()
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig mpc = MConfig.WithTraditionalDefaultSources
            .readUncachedResourceConfig( new String[] { vetoing, fine }, NONE, new ArrayList() );

        assertEquals( "from-plain", mpc.getProperty( "plain.key" ) );
        assertNull( mpc.getProperty( "secret.key" ) );
    }

    /** The traditional facade never throws a veto out to its caller. */
    public void testTraditionalNeverThrowsAVeto()
    {
        if ( !posixSupported() ) return;
        // no throws clause on this facade at all; this compiles only because nothing checked escapes
        MConfig.WithTraditionalDefaultSources
            .readUncachedResourceConfig( new String[] { vetoing }, NONE, new ArrayList() );
    }

    // ================================================ kind-specific messages

    /**
     *  The three kinds report a veto differently, and the wording matters: a veto is a bug only
     *  in the AsProvided case, where the up-front refusal should have made it unreachable.
     */
    public void testTraditionalVetoMessageDoesNotAccuseAnyoneOfABug()
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig mpc = MConfig.WithTraditionalDefaultSources
            .readUncachedResourceConfig( new String[] { vetoing }, NONE, new ArrayList() );

        assertTrue( "expected a plain 'was ignored' report, got:" + describe( mpc ),
                    hasWarningContaining( mpc, "ignored" ) );
        assertFalse( "a user's config choice is not a library bug, got:" + describe( mpc ),
                     hasWarningContaining( mpc, "bug in the com.mchange.v2.cfg library" ) );
    }

    // ============================================================== caching

    /**
     *  Cache entries are keyed by kind as well as by resolved paths, because the kinds disagree
     *  about what a veto means. A Traditional result must never be handed to a vetoable caller.
     */
    public void testCacheDistinguishesKinds() throws Exception
    {
        // must be a NON-vetoable identifier: AsProvided refuses every file: URL (see
        // testVetoabilityIsAPropertyOfTheSourceNotTheIdentifier), so a classpath resource it is
        String[] paths = new String[] { "/com/mchange/v2/cfg/junit/a.properties" };

        Object traditional  = MConfig.WithTraditionalDefaultSources.readCachedResourceConfig( paths, NONE );
        Object asProvided   = MConfig.AsProvided.readCachedResourceConfig( paths );
        Object vetoableRead = MConfig.AsProvidedVetoable.readCachedResourceConfig( paths );

        assertTrue( "traditional and as-provided must not share an entry", traditional != asProvided );
        assertTrue( "as-provided and vetoable must not share an entry",   asProvided != vetoableRead );

        assertSame( "repeats within one kind still share",
                    asProvided, MConfig.AsProvided.readCachedResourceConfig( paths ) );
        assertSame( "...and within the vetoable kind too",
                    vetoableRead, MConfig.AsProvidedVetoable.readCachedResourceConfig( paths ) );
    }

    // --------------------------------------------------- AsProvidedIgnoreVetoed reads

    // A non-vetoABLE survivor has to be a classpath resource: every file: URL is vetoable
    // whichever options it carries (see testVetoabilityIsAPropertyOfTheSourceNotTheIdentifier).
    // A non-vetoING file: URL is a different thing, and `fine` is one.
    private final static String SURVIVOR     = "/com/mchange/v2/cfg/junit/a.properties";
    private final static String SURVIVOR_KEY = "user.home";
    private final static String SURVIVOR_VAL = "/a/home";

    /**
     *  The point of the facade: AsProvided refuses a vetoable identifier outright, and this
     *  one accepts it and carries on.
     */
    public void testIgnoreVetoedAcceptsWhatAsProvidedRefuses()
    {
        try
        {
            MConfig.AsProvided.readUncachedResourceConfig( NONE, new String[] { vetoing }, new ArrayList() );
            fail( "AsProvided should still refuse a vetoable identifier" );
        }
        catch ( IllegalArgumentException expected )
        { /* the contrast this test exists for */ }

        assertNotNull( "the IgnoreVetoed read should have produced a config",
                       MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( NONE, new String[] { vetoing }, new ArrayList() ) );
    }

    /**
     *  And it does not abort the way AsProvidedVetoable does. Same input, same actual veto,
     *  two different answers -- which is the whole reason for a separate entry point.
     */
    public void testIgnoreVetoedNeverThrowsAVeto()
    {
        if ( !posixSupported() ) return;

        try
        {
            MConfig.AsProvidedVetoable.readUncachedResourceConfig( NONE, new String[] { vetoing }, new ArrayList() );
            fail( "AsProvidedVetoable should still throw" );
        }
        catch ( ConfigVetoedException expected )
        { /* the contrast this test exists for */ }

        MultiPropertiesConfig mpc = MConfig.AsProvidedIgnoreVetoed
            .readUncachedResourceConfig( NONE, new String[] { vetoing }, new ArrayList() );

        assertNotNull( mpc );
        assertNull( "the vetoing source must contribute nothing", mpc.getProperty( "secret.key" ) );
    }

    /** It must not drop anything else. The whole point is keeping the config you can have. */
    public void testIgnoreVetoedKeepsEverythingThatDidNotVeto()
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig mpc = MConfig.AsProvidedIgnoreVetoed
            .readUncachedResourceConfig( NONE, new String[] { SURVIVOR, vetoing }, new ArrayList() );

        assertEquals( "the non-vetoable source must survive", SURVIVOR_VAL, mpc.getProperty( SURVIVOR_KEY ) );
        assertTrue( "and stay in the resolved path list: " + Arrays.asList( mpc.getPropertiesResourcePaths() ),
                    Arrays.asList( mpc.getPropertiesResourcePaths() ).contains( SURVIVOR ) );
    }

    /**
     *  IGNORE VETOED, NOT VETOABLE. A file: source that loads cleanly must be honored in full.
     *  This is the distinction the facade is named for, and the thing an earlier
     *  "IgnoreVetoable" shape got wrong by filtering on source type before reading anything.
     */
    public void testIgnoreVetoedHonorsAFileSourceThatDoesNotVeto()
    {
        MultiPropertiesConfig mpc = MConfig.AsProvidedIgnoreVetoed
            .readUncachedResourceConfig( NONE, new String[] { fine }, new ArrayList() );

        assertEquals( "a file: source that never vetoes must load normally",
                      "from-plain", mpc.getProperty( "plain.key" ) );
        assertTrue( "and stay in the resolved path list: " + Arrays.asList( mpc.getPropertiesResourcePaths() ),
                    Arrays.asList( mpc.getPropertiesResourcePaths() ).contains( fine ) );
    }

    /** Silently losing configuration would be worse than failing. It must say what went. */
    public void testIgnoreVetoedWarnsNamingWhatItDropped()
    {
        if ( !posixSupported() ) return;

        List items = new ArrayList();
        MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( NONE, new String[] { SURVIVOR, vetoing }, items );

        assertTrue( "expected a WARNING naming the dropped identifier " + vetoing + ", got:" + describeItems( items ),
                    hasWarningMentioning( items, vetoing ) );
    }

    /**
     *  ...and must NOT complain when nothing vetoed. A facade that warns on every read trains
     *  its users to ignore the one warning that matters.
     */
    public void testIgnoreVetoedIsSilentWhenNothingVetoes()
    {
        List items = new ArrayList();
        MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( NONE, new String[] { SURVIVOR }, items );

        assertFalse( "no veto occurred, so nothing should be reported as ignored, got:" + describeItems( items ),
                     hasWarningMentioning( items, "vetoed" ) );
        assertFalse( hasWarningMentioning( items, "ignored" ) );
    }

    /**
     *  The message must not accuse anyone of a bug. Kind.AsProvided's veto arm does exactly
     *  that -- deliberately, because a veto there means the up-front guard failed -- so a
     *  facade built by dropping that guard must not reuse it. Compare
     *  testTraditionalVetoMessageDoesNotAccuseAnyoneOfABug.
     */
    public void testIgnoreVetoedMessageDoesNotAccuseAnyoneOfABug()
    {
        if ( !posixSupported() ) return;

        List items = new ArrayList();
        MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( NONE, new String[] { vetoing }, items );

        assertFalse( "a deliberate IgnoreVetoed read must not report a library bug, got:" + describeItems( items ),
                     hasWarningMentioning( items, "bug in the com.mchange.v2.cfg library" ) );
        assertTrue( "it should report the veto plainly instead, got:" + describeItems( items ),
                    hasWarningMentioning( items, "has vetoed config" ) );
    }

    /**
     *  Cached and uncached forms of one facade must resolve the same paths. They condense
     *  their path lists in different places, so nothing but a test keeps them in step.
     */
    public void testIgnoreVetoedCachedAgreesWithUncached()
    {
        String[] paths = new String[] { SURVIVOR };

        MultiPropertiesConfig uncached = MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( paths );
        MultiPropertiesConfig cached   = MConfig.AsProvidedIgnoreVetoed.readCachedResourceConfig( paths );

        assertEquals( "same resolved path list",
                      Arrays.asList( uncached.getPropertiesResourcePaths() ),
                      Arrays.asList( cached.getPropertiesResourcePaths() ) );
        assertEquals( "same surviving value",
                      uncached.getProperty( SURVIVOR_KEY ), cached.getProperty( SURVIVOR_KEY ) );
    }

    /**
     *  As-provided means as provided: only the caller's paths. The facade sits beside
     *  AsProvided, not Traditional, so it must not reach for resource-path text files or the
     *  hardcoded backstop.
     */
    public void testIgnoreVetoedUsesOnlyTheCallersPaths()
    {
        String[] paths = new String[] { SURVIVOR };

        assertEquals( "AsProvidedIgnoreVetoed should resolve exactly the paths AsProvided does",
                      Arrays.asList( MConfig.AsProvided.readUncachedResourceConfig( NONE, paths, new ArrayList() ).getPropertiesResourcePaths() ),
                      Arrays.asList( MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( NONE, paths, new ArrayList() ).getPropertiesResourcePaths() ) );
    }

    /** Null path arrays, as AsProvided already tolerates them. */
    public void testIgnoreVetoedToleratesNullPathArrays()
    {
        assertNotNull( MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig( null, null, new ArrayList() ) );

        assertEquals( SURVIVOR_VAL, MConfig.AsProvidedIgnoreVetoed
                      .readUncachedResourceConfig( null, new String[] { SURVIVOR }, new ArrayList() ).getProperty( SURVIVOR_KEY ) );
        assertEquals( SURVIVOR_VAL, MConfig.AsProvidedIgnoreVetoed
                      .readUncachedResourceConfig( new String[] { SURVIVOR }, null, new ArrayList() ).getProperty( SURVIVOR_KEY ) );
    }

    private static boolean hasWarningMentioning( List items, String fragment )
    {
        for ( Object o : items )
        {
            DelayedLogItem item = (DelayedLogItem) o;
            if ( DelayedLogItem.Level.WARNING.equals( item.getLevel() )
                 && item.getText() != null && item.getText().contains( fragment ) )
                return true;
        }
        return false;
    }

    private static String describeItems( List items )
    {
        StringBuilder sb = new StringBuilder();
        for ( Object o : items )
        {
            DelayedLogItem item = (DelayedLogItem) o;
            sb.append( "\n    [" ).append( item.getLevel() ).append( "] " ).append( item.getText() );
        }
        return sb.toString();
    }
}
