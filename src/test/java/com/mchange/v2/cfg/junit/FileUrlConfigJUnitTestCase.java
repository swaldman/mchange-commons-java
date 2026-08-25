package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import com.mchange.v2.cfg.ConfigVetoedException;
import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.InsecureConfigurationException;
import com.mchange.v2.cfg.MConfig;
import com.mchange.v2.cfg.MultiPropertiesConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  The {@code file:} resource-path scheme -- loading properties from an absolute filesystem
 *  path -- and its query-string options {@code permissions=useronly} and {@code required}.
 *
 *  A file: source is a VetoableConfig, so these reads go through MConfig.AsProvidedVetoable.
 *  What a veto MEANS to each facade is covered separately, in VetoableConfigJUnitTestCase;
 *  here we are only asking what makes this particular source veto.
 *
 *  No scenario ClassLoader is needed -- a file URL's behavior does not depend on classpath
 *  composition -- but real files with real POSIX modes are, so the fixture is a temp directory
 *  built per test. Permission-sensitive assertions are skipped where POSIX is unavailable.
 */
public final class FileUrlConfigJUnitTestCase extends TestCase
{
    private Path dir;
    private Path userOnly;   // 0600  secret.key=from-useronly
    private Path worldRead;  // 0644  secret.key=from-worldread
    private Path second;     // 0600  secret.key=from-second

    protected void setUp() throws Exception
    {
        // toRealPath: on macOS the temp dir sits under /var, which is itself a symlink, and these
        // tests reason about link chains -- we want the paths we think we have
        dir       = Files.createTempDirectory( "mchange-cfg-fileurl-" ).toRealPath();
        userOnly  = write( "useronly.properties",  "secret.key=from-useronly\nonly.useronly=yes\n", "rw-------" );
        worldRead = write( "worldread.properties", "secret.key=from-worldread\n",                   "rw-r--r--" );
        second    = write( "second.properties",    "secret.key=from-second\nonly.second=yes\n",     "rw-------" );
    }

    protected void tearDown() throws Exception
    {
        deleteQuietly( userOnly ); deleteQuietly( worldRead ); deleteQuietly( second ); deleteQuietly( dir );
    }

    // ------------------------------------------------------------- fixture

    private Path write( String name, String contents, String mode ) throws IOException
    {
        Path p = dir.resolve( name );
        Files.write( p, contents.getBytes( "8859_1" ) );
        if ( posixSupported() ) Files.setPosixFilePermissions( p, PosixFilePermissions.fromString( mode ) );
        return p;
    }

    private Path symlink( String name, Path target ) throws IOException
    {
        Path link = dir.resolve( name );
        Files.createSymbolicLink( link, target );
        return link;
    }

    private Path hardlink( String name, Path target ) throws IOException
    {
        Path link = dir.resolve( name );
        Files.createLink( link, target );
        return link;
    }

    private static void deleteQuietly( Path p )
    { try { if ( p != null ) Files.deleteIfExists( p ); } catch ( IOException e ) { /* best effort */ } }

    private boolean posixSupported()
    { return dir.getFileSystem().supportedFileAttributeViews().contains( "posix" ); }

    private String url( Path p, String query )
    { return p.toUri().toString() + (query == null ? "" : "?" + query); }

    private String url( Path p )
    { return url( p, null ); }

    private String missingUrl( String query )
    { return url( dir.resolve( "definitely-absent.properties" ), query ); }

    // ------------------------------------------------------------- helpers

    /** file: sources are vetoable, so reads of them go through the vetoable facade. */
    private static MultiPropertiesConfig read( String... paths ) throws ConfigVetoedException
    { return MConfig.AsProvidedVetoable.readUncachedClassloaderResourceConfig( paths, new ArrayList() ); }

    private static List<String> pathsOf( MultiPropertiesConfig mpc )
    { return Arrays.asList( mpc.getPropertiesResourcePaths() ); }

    /** Asserts the read is vetoed, and returns the veto's message. */
    private static String assertVetoed( String... paths )
    {
        try
        {
            MultiPropertiesConfig mpc = read( paths );
            fail( "expected a veto, but the read succeeded with " + Arrays.toString( mpc.getPropertiesResourcePaths() ) );
            return null; // unreachable
        }
        catch ( ConfigVetoedException expected )
        {
            assertTrue( "a permissions/required refusal should be an InsecureConfigurationException, was "
                            + expected.getClass().getName(),
                        expected instanceof InsecureConfigurationException );
            return expected.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean hasFineSkip( MultiPropertiesConfig mpc )
    {
        List<DelayedLogItem> items = mpc.getDelayedLogItems(); // raw List on the API
        for ( DelayedLogItem item : items )
            if ( DelayedLogItem.Level.FINE.equals( item.getLevel() )
                 && item.getText() != null && item.getText().contains( "could not be found. Skipping." ) )
                return true;
        return false;
    }

    // =================================================== basic functionality

    public void testFileUrlPropertiesAppearInConfig() throws Exception
    {
        MultiPropertiesConfig mpc = read( url( userOnly ) );

        assertEquals( "from-useronly", mpc.getProperty( "secret.key" ) );
        assertEquals( "yes", mpc.getProperty( "only.useronly" ) );
        assertEquals( Arrays.asList( url( userOnly ) ), pathsOf( mpc ) );
    }

    public void testFileUrlWithQueryStringStillLoads() throws Exception
    { assertEquals( "from-useronly", read( url( userOnly, "permissions=useronly" ) ).getProperty( "secret.key" ) ); }

    public void testLaterFileUrlWinsOnConflict() throws Exception
    {
        MultiPropertiesConfig mpc = read( url( userOnly ), url( second ) );

        assertEquals( "the later file URL should win", "from-second", mpc.getProperty( "secret.key" ) );
        assertEquals( "yes", mpc.getProperty( "only.useronly" ) );
        assertEquals( "yes", mpc.getProperty( "only.second" ) );
    }

    public void testFileUrlOrderIsSignificant() throws Exception
    { assertEquals( "from-useronly", read( url( second ), url( userOnly ) ).getProperty( "secret.key" ) ); }

    /** file: and classpath-resource sources layer together, later winning. */
    public void testFileUrlLayersWithClasspathResources() throws Exception
    {
        final String RSRC_A = "/com/mchange/v2/cfg/junit/a.properties"; // user.home=/a/home
        Path homeOverride = write( "home.properties", "user.home=/from/file/url\n", "rw-------" );
        try
        {
            assertEquals( "the file URL listed later should win",
                          "/from/file/url", read( RSRC_A, url( homeOverride ) ).getProperty( "user.home" ) );
            assertEquals( "the classpath resource listed later should win",
                          "/a/home", read( url( homeOverride ), RSRC_A ).getProperty( "user.home" ) );
        }
        finally
        { deleteQuietly( homeOverride ); }
    }

    /**
     *  Precedence between the two non-classpath schemes, in both directions.
     *
     *  file:-vs-classpath and hocon:-vs-classpath are each covered elsewhere, so file:-vs-hocon
     *  follows transitively -- but only if precedence really is a property of position in the
     *  resolved path list rather than of the kind of source. This asserts it directly.
     *
     *  A HOCON element containing a colon is read as a URL, so the HOCON side can be a temp file
     *  too, and the test needs no build-time classpath resource.
     */
    public void testFileUrlAndHoconPathLayerInPositionOrder() throws Exception
    {
        Path conf = dir.resolve( "layered.conf" );
        Files.write( conf, "shared.key = from-hocon\nonly.hocon = yes\n".getBytes( "8859_1" ) );

        Path props = write( "layered.properties", "shared.key=from-file-url\nonly.fileurl=yes\n", "rw-------" );

        String hoconPath = "hocon:" + conf.toUri().toString();
        String fileUrl   = url( props );

        try
        {
            MultiPropertiesConfig hoconFirst = read( hoconPath, fileUrl );
            assertEquals( "the file URL is later, so it wins",
                          "from-file-url", hoconFirst.getProperty( "shared.key" ) );
            assertEquals( "the HOCON source still contributes its own keys",
                          "yes", hoconFirst.getProperty( "only.hocon" ) );
            assertEquals( "yes", hoconFirst.getProperty( "only.fileurl" ) );

            MultiPropertiesConfig fileFirst = read( fileUrl, hoconPath );
            assertEquals( "reversed, the HOCON path is later, so it wins",
                          "from-hocon", fileFirst.getProperty( "shared.key" ) );
            assertEquals( "yes", fileFirst.getProperty( "only.hocon" ) );
            assertEquals( "yes", fileFirst.getProperty( "only.fileurl" ) );
        }
        finally
        { deleteQuietly( conf ); deleteQuietly( props ); }
    }

    // ==================================================== permissions matrix

    public void testUserOnlyPermissionsAccepted() throws Exception
    {
        if ( !posixSupported() ) return;
        assertEquals( "from-useronly", read( url( userOnly, "permissions=useronly" ) ).getProperty( "secret.key" ) );
    }

    public void testWorldReadableFileIsVetoed()
    {
        if ( !posixSupported() ) return;
        String msg = assertVetoed( url( worldRead, "permissions=useronly" ) );
        assertTrue( "the message should name the offending permissions, was: " + msg,
                    msg.contains( "OTHERS_READ" ) || msg.contains( "GROUP_READ" ) );
    }

    public void testWorldReadableFileLoadsWithoutTheQuery() throws Exception
    {
        assertEquals( "permissions should only be checked when asked for",
                      "from-worldread", read( url( worldRead ) ).getProperty( "secret.key" ) );
    }

    public void testPermissionsValueIsCaseInsensitive() throws Exception
    {
        if ( !posixSupported() ) return;
        assertVetoed( url( worldRead, "permissions=UserOnly" ) ); // understood and enforced, not ignored
        assertEquals( "from-useronly", read( url( userOnly, "permissions=USERONLY" ) ).getProperty( "secret.key" ) );
    }

    /**
     *  The query KEY is case-sensitive, and a mis-cased key vetoes rather than being ignored.
     *  This is the fail-open case: a capitalized key must never quietly skip the permissions
     *  check on a world-readable file.
     */
    public void testMisCasedPermissionsKeyIsVetoedNotIgnored()
    {
        String msg = assertVetoed( url( worldRead, "Permissions=useronly" ) );
        assertTrue( "the message should name the offending key, was: " + msg, msg.contains( "Permissions" ) );
    }

    public void testPermissionsKeyWithNoValueIsVetoed()
    {
        String msg = assertVetoed( url( worldRead, "permissions" ) );
        assertTrue( "should mention the missing value, was: " + msg, msg.contains( "no value" ) );
    }

    public void testEmptyPermissionsValueIsVetoed()
    { assertVetoed( url( worldRead, "permissions=" ) ); }

    public void testUnsupportedPermissionsValueIsVetoed()
    {
        String msg = assertVetoed( url( worldRead, "permissions=everyone" ) );
        assertTrue( "should name the offending value, was: " + msg, msg.contains( "everyone" ) );
    }

    public void testUnsupportedQueryKeyIsVetoed()
    {
        String msg = assertVetoed( url( userOnly, "permissoins=useronly" ) );
        assertTrue( "should name the offending key, was: " + msg, msg.contains( "permissoins" ) );
    }

    /** A veto aborts the whole read -- it does not merely drop the offending path. */
    public void testVetoAbortsTheEntireRead()
    {
        if ( !posixSupported() ) return;
        // the good source is listed FIRST, so "abort" is distinguishable from "skip the bad one"
        assertVetoed( url( userOnly ), url( worldRead, "permissions=useronly" ) );
    }

    /** The veto carries the source and identifier that produced it, not just a message. */
    public void testVetoIdentifiesItsSourceAndIdentifier()
    {
        if ( !posixSupported() ) return;
        String id = url( worldRead, "permissions=useronly" );
        try
        {
            read( id );
            fail( "expected a veto" );
        }
        catch ( ConfigVetoedException cve )
        {
            assertEquals( "the veto should report the identifier it choked on", id, cve.getIdentifier() );
            assertNotNull( "the veto should report the source that raised it", cve.getSource() );
        }
    }

    /**
     *  A successful useronly read reports nothing. The source now carries DelayedLogItems out on
     *  its Parse -- it must not chatter on the ordinary path, only when the superuser could not be
     *  identified numerically and a weaker name lookup was used instead.
     */
    public void testSuccessfulUserOnlyReadReportsNothing() throws Exception
    {
        if ( !posixSupported() ) return;

        MultiPropertiesConfig mpc = read( url( userOnly, "permissions=useronly" ) );

        assertEquals( "from-useronly", mpc.getProperty( "secret.key" ) );
        List<DelayedLogItem> items = mpc.getDelayedLogItems();
        for ( DelayedLogItem item : items )
            assertFalse( "no superuser-fallback warning should be emitted here: " + item.getText(),
                         item.getText() != null && item.getText().contains( "superuser" ) );
    }

    // ============================================== links: symbolic and hard

    /**
     *  useronly follows symbolic links rather than refusing them -- linking config into place is
     *  ordinary practice -- but it verifies the whole chain: every link must be owned by us (or
     *  root), and the file finally reached must have no group or other permission bits.
     *
     *  Only the final file's MODE is examined. A link's own mode is not a usable signal: it
     *  defaults to rwxr-xr-x here, and on Linux link modes are fixed at rwxrwxrwx and ignored by
     *  the kernel. Checking it would refuse nearly every symlink.
     */
    public void testSymlinkToPrivateFileLoads() throws Exception
    {
        if ( !posixSupported() ) return;
        Path link = symlink( "link-to-private", userOnly );
        try
        { assertEquals( "from-useronly", read( url( link, "permissions=useronly" ) ).getProperty( "secret.key" ) ); }
        finally
        { deleteQuietly( link ); }
    }

    /** The motivating case: a link is not allowed to launder an exposed file. */
    public void testSymlinkToWorldReadableFileIsVetoed() throws Exception
    {
        if ( !posixSupported() ) return;
        Path link = symlink( "link-to-world", worldRead );
        try
        {
            String msg = assertVetoed( url( link, "permissions=useronly" ) );
            assertTrue( "should name the exposed TARGET's permissions, was: " + msg,
                        msg.contains( "OTHERS_READ" ) || msg.contains( "GROUP_READ" ) );
            assertTrue( "should show the chain that was walked, was: " + msg, msg.contains( "reached via" ) );
        }
        finally
        { deleteQuietly( link ); }
    }

    /**
     *  And a link whose own mode has been restricted must not launder it either. On macOS/BSD
     *  `chmod -h` can give a symlink owner-only bits, which defeats any check that merely reads
     *  permissions with NOFOLLOW_LINKS. Following through to the target is what catches it.
     */
    public void testSymlinkWithRestrictedOwnModeStillChecksItsTarget() throws Exception
    {
        if ( !posixSupported() ) return;
        Path link = symlink( "link-restricted", worldRead );
        try
        {
            try { Files.setAttribute( link, "posix:permissions",
                                      PosixFilePermissions.fromString( "rw-------" ),
                                      LinkOption.NOFOLLOW_LINKS ); }
            catch ( Exception e ) { /* platforms that pin link modes -- the point holds either way */ }

            assertVetoed( url( link, "permissions=useronly" ) );
        }
        finally
        { deleteQuietly( link ); }
    }

    /** Every hop is checked, not just the first. */
    public void testMultiHopSymlinkChainLoads() throws Exception
    {
        if ( !posixSupported() ) return;
        Path hop2 = symlink( "hop2", userOnly );
        Path hop1 = symlink( "hop1", hop2 );
        try
        { assertEquals( "from-useronly", read( url( hop1, "permissions=useronly" ) ).getProperty( "secret.key" ) ); }
        finally
        { deleteQuietly( hop1 ); deleteQuietly( hop2 ); }
    }

    /** A link target may be relative, and resolves against the link's directory. */
    public void testRelativeSymlinkTargetResolves() throws Exception
    {
        if ( !posixSupported() ) return;
        Path link = dir.resolve( "relative-link" );
        Files.createSymbolicLink( link, Paths.get( userOnly.getFileName().toString() ) );
        try
        { assertEquals( "from-useronly", read( url( link, "permissions=useronly" ) ).getProperty( "secret.key" ) ); }
        finally
        { deleteQuietly( link ); }
    }

    /**
     *  Hard links need no special handling and get none: a hard link IS the file, sharing its
     *  inode, owner and permissions, so it arrives at the check as a regular file.
     */
    public void testHardLinkToPrivateFileLoads() throws Exception
    {
        if ( !posixSupported() ) return;
        Path hard = hardlink( "hard-to-private", userOnly );
        try
        { assertEquals( "from-useronly", read( url( hard, "permissions=useronly" ) ).getProperty( "secret.key" ) ); }
        finally
        { deleteQuietly( hard ); }
    }

    /** ...which means a hard link to an exposed file is caught for free. */
    public void testHardLinkToWorldReadableFileIsVetoed() throws Exception
    {
        if ( !posixSupported() ) return;
        Path hard = hardlink( "hard-to-world", worldRead );
        try
        {
            String msg = assertVetoed( url( hard, "permissions=useronly" ) );
            assertTrue( "should name the exposed permissions, was: " + msg,
                        msg.contains( "OTHERS_READ" ) || msg.contains( "GROUP_READ" ) );
        }
        finally
        { deleteQuietly( hard ); }
    }

    /** A cycle of links must be refused, and must not spin. */
    public void testSymlinkLoopIsVetoedAndDoesNotHang() throws Exception
    {
        if ( !posixSupported() ) return;
        Path a = dir.resolve( "loop-a" );
        Path b = dir.resolve( "loop-b" );
        Files.createSymbolicLink( a, b );
        Files.createSymbolicLink( b, a );
        try
        {
            String msg = assertVetoed( url( a, "permissions=useronly" ) );
            assertTrue( "should say it is a cycle, was: " + msg, msg.contains( "cycle" ) );
        }
        finally
        { deleteQuietly( a ); deleteQuietly( b ); }
    }

    /** A dangling link is absence, not insecurity -- it is skipped, exactly as a missing file is. */
    public void testDanglingSymlinkIsSkippedNotVetoed() throws Exception
    {
        if ( !posixSupported() ) return;
        Path link = dir.resolve( "dangling" );
        Files.createSymbolicLink( link, dir.resolve( "never-created.properties" ) );
        try
        {
            MultiPropertiesConfig mpc = read( url( link, "permissions=useronly" ) );
            assertEquals( 0, pathsOf( mpc ).size() );
            assertTrue( "expected a FINE skip, not a veto", hasFineSkip( mpc ) );
        }
        finally
        { deleteQuietly( link ); }
    }

    /** ...but a dangling link IS a veto when the config was declared required. */
    public void testDanglingSymlinkIsVetoedWhenRequired() throws Exception
    {
        if ( !posixSupported() ) return;
        Path link = dir.resolve( "dangling-required" );
        Files.createSymbolicLink( link, dir.resolve( "never-created.properties" ) );
        try
        { assertTrue( assertVetoed( url( link, "permissions=useronly&required=true" ) ).contains( "required" ) ); }
        finally
        { deleteQuietly( link ); }
    }

    /** Without the permissions query, link structure is irrelevant -- nothing is examined. */
    public void testLinksAreUncheckedWithoutThePermissionsQuery() throws Exception
    {
        if ( !posixSupported() ) return;
        Path link = symlink( "unchecked-link", worldRead );
        Path hard = hardlink( "unchecked-hard", worldRead );
        try
        {
            assertEquals( "from-worldread", read( url( link ) ).getProperty( "secret.key" ) );
            assertEquals( "from-worldread", read( url( hard ) ).getProperty( "secret.key" ) );
        }
        finally
        { deleteQuietly( link ); deleteQuietly( hard ); }
    }

    // NOTE: a link owned by a DIFFERENT user is the remaining case, and cannot be built in a
    // single-uid test. The rule is that a link must be owned by the running user or by root,
    // since an attacker cannot chown a link to someone else.

    // ================================================= missing-file handling

    public void testMissingFileIsSkipped() throws Exception
    {
        MultiPropertiesConfig mpc = read( missingUrl( null ) );
        assertEquals( 0, pathsOf( mpc ).size() );
        assertTrue( "expected a FINE skip notice", hasFineSkip( mpc ) );
    }

    /**
     *  Absence is not insecurity: a missing file must be skipped even when
     *  permissions=useronly is requested, rather than vetoing.
     */
    public void testMissingFileWithPermissionsQueryIsAlsoJustSkipped() throws Exception
    {
        MultiPropertiesConfig mpc = read( missingUrl( "permissions=useronly" ) );
        assertEquals( 0, pathsOf( mpc ).size() );
        assertTrue( "expected a FINE skip, not a veto", hasFineSkip( mpc ) );
    }

    public void testMissingFileDoesNotDisturbOtherSources() throws Exception
    {
        MultiPropertiesConfig mpc = read( missingUrl( "permissions=useronly" ), url( userOnly ) );

        assertEquals( "from-useronly", mpc.getProperty( "secret.key" ) );
        assertEquals( Arrays.asList( url( userOnly ) ), pathsOf( mpc ) );
    }

    // ============================================================= required

    public void testRequiredTrueWithPresentFileLoads() throws Exception
    { assertEquals( "from-useronly", read( url( userOnly, "required=true" ) ).getProperty( "secret.key" ) ); }

    public void testRequiredTrueWithMissingFileIsVetoed()
    {
        String msg = assertVetoed( missingUrl( "required=true" ) );
        assertTrue( "should say the file was required, was: " + msg, msg.contains( "required" ) );
    }

    public void testRequiredFalseWithMissingFileIsSkipped() throws Exception
    {
        MultiPropertiesConfig mpc = read( missingUrl( "required=false" ) );
        assertEquals( 0, pathsOf( mpc ).size() );
        assertTrue( hasFineSkip( mpc ) );
    }

    public void testRequiredAndPermissionsCompose() throws Exception
    {
        if ( !posixSupported() ) return;

        assertEquals( "both satisfied -> loads", "from-useronly",
                      read( url( userOnly, "permissions=useronly&required=true" ) ).getProperty( "secret.key" ) );

        // present but insecure, and required: the permissions failure is what reports
        String msg = assertVetoed( url( worldRead, "permissions=useronly&required=true" ) );
        assertTrue( "expected the permissions failure, was: " + msg,
                    msg.contains( "OTHERS_READ" ) || msg.contains( "GROUP_READ" ) );

        // absent and required: required is what reports
        assertTrue( assertVetoed( missingUrl( "permissions=useronly&required=true" ) ).contains( "required" ) );
    }

    public void testMalformedRequiredValueIsVetoed()
    {
        assertVetoed( url( userOnly, "required" ) );
        assertVetoed( url( userOnly, "required=" ) );
        assertVetoed( url( userOnly, "required=yes" ) );
        assertVetoed( url( userOnly, "required=true&required=false" ) );
    }

    // ------------------------------------------------ capitalized scheme

    public void testCapitalizedSchemeIsHonored() throws Exception
    {
        String upper = "FILE:" + url( userOnly ).substring( "file:".length() );
        assertEquals( "from-useronly", read( upper ).getProperty( "secret.key" ) );
    }

    public void testCapitalizedSchemeStillEnforcesPermissions()
    {
        if ( !posixSupported() ) return;
        assertVetoed( "FILE:" + url( worldRead, "permissions=useronly" ).substring( "file:".length() ) );
    }
}
