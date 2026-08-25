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
 *
 *  The fixture is a world-readable file asked to be user-only, which is the simplest thing that
 *  reliably vetoes. FileUrlConfigJUnitTestCase covers WHY that source vetoes; this covers what
 *  each facade then does about it.
 */
public final class VetoableConfigJUnitTestCase extends TestCase
{
    private Path   dir;
    private Path   worldRead;   // 0644, asked to be useronly -> vetoes
    private Path   plain;       // ordinary, loads fine
    private String vetoing;
    private String fine;

    private final static String[] NONE = new String[0];

    protected void setUp() throws Exception
    {
        dir       = Files.createTempDirectory( "mchange-cfg-veto-" );
        worldRead = write( "worldread.properties", "secret.key=from-worldread\n", "rw-r--r--" );
        plain     = write( "plain.properties",     "plain.key=from-plain\n",      "rw-------" );
        vetoing   = worldRead.toUri().toString() + "?permissions=useronly";
        fine      = plain.toUri().toString();
    }

    protected void tearDown() throws Exception
    { deleteQuietly( worldRead ); deleteQuietly( plain ); deleteQuietly( dir ); }

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

    // ==================================================== AsProvidedVetoable

    /** The opt-in facade: a veto aborts the read, as a checked exception. */
    public void testVetoableFacadeThrows()
    {
        if ( !posixSupported() ) return;
        try
        {
            MConfig.AsProvidedVetoable.readUncachedClassloaderResourceConfig( new String[] { vetoing }, new ArrayList() );
            fail( "expected a ConfigVetoedException" );
        }
        catch ( ConfigVetoedException expected )
        { assertEquals( vetoing, expected.getIdentifier() ); }
    }

    /** Non-vetoing paths read normally through the vetoable facade. */
    public void testVetoableFacadeReadsNormallyWhenNothingVetoes() throws Exception
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvidedVetoable.readUncachedClassloaderResourceConfig( new String[] { fine }, new ArrayList() );
        assertEquals( "from-plain", mpc.getProperty( "plain.key" ) );
    }

    /** The veto also survives the cached path with its own type intact. */
    public void testVetoableFacadeThrowsFromCachedReadsToo()
    {
        if ( !posixSupported() ) return;
        try
        {
            MConfig.AsProvidedVetoable.readCachedClassloaderResourceConfig( new String[] { vetoing } );
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
            MConfig.AsProvided.readUncachedClassloaderResourceConfig( new String[] { vetoing }, new ArrayList() );
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
            MConfig.AsProvided.readUncachedClassloaderResourceConfig( new String[] { absent }, new ArrayList() );
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
            MConfig.AsProvided.readUncachedClassloaderResourceConfig( new String[] { fine, vetoing }, new ArrayList() );
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
            MConfig.AsProvided.readUncachedClassloaderResourceConfig( new String[] { fine }, new ArrayList() );
            fail( "expected IllegalArgumentException even for an option-free file: URL" );
        }
        catch ( IllegalArgumentException expected )
        { assertTrue( expected.getMessage().contains( "AsProvidedVetoable" ) ); }
    }

    /** Non-vetoable identifiers pass through untouched. */
    public void testAsProvidedAcceptsNonVetoableIdentifiers()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readUncachedClassloaderResourceConfig( new String[] { "/" }, new ArrayList() );
        assertEquals( System.getProperty( "user.home" ), mpc.getProperty( "user.home" ) );
    }

    /** Null path arrays are tolerated -- the vetoable check runs before nulls are normalized. */
    public void testAsProvidedToleratesNullPathArrays()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readUncachedClassloaderResourceConfig( null, null, new ArrayList() );
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
            .readUncachedClassloaderResourceConfig( new String[] { fine, vetoing }, NONE, new ArrayList() );

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
            .readUncachedClassloaderResourceConfig( new String[] { vetoing, fine }, NONE, new ArrayList() );

        assertEquals( "from-plain", mpc.getProperty( "plain.key" ) );
        assertNull( mpc.getProperty( "secret.key" ) );
    }

    /** The traditional facade never throws a veto out to its caller. */
    public void testTraditionalNeverThrowsAVeto()
    {
        if ( !posixSupported() ) return;
        // no throws clause on this facade at all; this compiles only because nothing checked escapes
        MConfig.WithTraditionalDefaultSources
            .readUncachedClassloaderResourceConfig( new String[] { vetoing }, NONE, new ArrayList() );
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
            .readUncachedClassloaderResourceConfig( new String[] { vetoing }, NONE, new ArrayList() );

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

        Object traditional  = MConfig.WithTraditionalDefaultSources.readCachedClassloaderResourceConfig( paths, NONE );
        Object asProvided   = MConfig.AsProvided.readCachedClassloaderResourceConfig( paths );
        Object vetoableRead = MConfig.AsProvidedVetoable.readCachedClassloaderResourceConfig( paths );

        assertTrue( "traditional and as-provided must not share an entry", traditional != asProvided );
        assertTrue( "as-provided and vetoable must not share an entry",   asProvided != vetoableRead );

        assertSame( "repeats within one kind still share",
                    asProvided, MConfig.AsProvided.readCachedClassloaderResourceConfig( paths ) );
        assertSame( "...and within the vetoable kind too",
                    vetoableRead, MConfig.AsProvidedVetoable.readCachedClassloaderResourceConfig( paths ) );
    }
}
