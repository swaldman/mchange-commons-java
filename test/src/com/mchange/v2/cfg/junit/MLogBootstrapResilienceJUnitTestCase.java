package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 *  The claim the VetoableConfig design exists to make good on: an end user's configuration
 *  choice cannot prevent the logging library from starting.
 *
 *  MLog reads its own configuration during class initialization, through
 *  MConfig.WithTraditionalDefaultSources, and that read consults resource-path text files
 *  (/mchange-config-resource-paths.txt and friends) which the application does not control.
 *  If someone drops a file: URL naming a world-readable secrets file into one of those, the
 *  vetoing source must be ignored with a warning rather than taking logging down with it.
 *
 *  Each case builds a scenario classpath containing a real path file that names a real temp
 *  file, then initializes MLog inside it and asks only whether the class came up. The scenario
 *  ClassLoader is what makes this testable in-process: MLog is loaded fresh per case, so its
 *  one-shot static initializer runs once per scenario rather than once per JVM.
 */
public final class MLogBootstrapResilienceJUnitTestCase extends TestCase
{
    private Path dir;

    @Override
    protected void setUp() throws Exception
    { dir = Files.createTempDirectory( "mchange-cfg-bootstrap-" ); }

    @Override
    protected void tearDown() throws Exception
    {
        if ( dir != null )
        {
            File[] kids = dir.toFile().listFiles();
            if ( kids != null ) for ( File k : kids ) k.delete();
            dir.toFile().delete();
        }
    }

    private boolean posixSupported()
    { return dir.getFileSystem().supportedFileAttributeViews().contains( "posix" ); }

    /** Writes a config file, plus a path file naming it, into a fresh classpath root. */
    private File rootNaming( String configContents, String mode, String queryString ) throws IOException
    {
        Path root    = Files.createDirectory( dir.resolve( "root-" + System.nanoTime() ) );
        Path payload = dir.resolve( "payload-" + System.nanoTime() + ".properties" );

        Files.write( payload, configContents.getBytes( "8859_1" ) );
        if ( mode != null && posixSupported() )
            Files.setPosixFilePermissions( payload, PosixFilePermissions.fromString( mode ) );

        String identifier = payload.toUri().toString() + (queryString == null ? "" : "?" + queryString);
        Files.write( root.resolve( "mchange-config-resource-paths.txt" ),
                     (identifier + "\n").getBytes( "8859_1" ) );
        return root.toFile();
    }

    /** Initializes MLog inside a scenario, and reports what happened. */
    private static Throwable initMLogIn( CfgScenario s )
    {
        try
        {
            Class.forName( "com.mchange.v2.log.MLog", true, s.classLoader() );
            return null;
        }
        catch ( Throwable t )
        { return t; }
    }

    private void assertMLogComesUp( String label, File root )
    {
        CfgScenario s = CfgScenario.openWithRoots( "no-pathfiles", false, root );
        try
        {
            Throwable failure = initMLogIn( s );
            assertNull( label + ": MLog must initialize despite the configuration, but got " + failure,
                        failure );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  Teeth for the cases below: confirms the poison actually reaches the config machinery.
     *
     *  Without this, "MLog came up" would pass just as happily if the path file were never read
     *  at all -- so assert that the veto genuinely fires, that the offending path contributes
     *  nothing, and that it is reported as ignored rather than as an error.
     */
    public void testThePoisonReallyDoesVeto() throws Exception
    {
        if ( !posixSupported() ) return;

        File root = rootNaming( "secret.key=exposed\n", "rw-r--r--", "permissions=useronly" );
        CfgScenario s = CfgScenario.openWithRoots( "no-pathfiles", false, root );
        try
        {
            Object mpc = s.traditionalUncached( new String[0], new String[0], new java.util.ArrayList() );

            assertNull( "the world-readable file must contribute nothing",
                        s.getProperty( mpc, "secret.key" ) );
            assertEquals( "and its path must be dropped",
                          0, s.getPropertiesResourcePaths( mpc ).length );

            java.util.List<String[]> items = s.getDelayedLogItems( mpc );
            assertTrue( "expected a WARNING reporting the veto, got:" + CfgScenario.describe( items ),
                        CfgScenario.hasLogItem( items, "WARNING", "has vetoed config" ) );
            assertFalse( "an end user's config choice is not a library bug:" + CfgScenario.describe( items ),
                         CfgScenario.hasLogItem( items, "WARNING", "bug in the com.mchange.v2.cfg library" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Sanity: a well-formed path file naming a readable file does not disturb anything. */
    public void testHealthyFileUrlInPathFileIsFine() throws Exception
    { assertMLogComesUp( "healthy file: URL", rootNaming( "some.key=value\n", "rw-------", null ) ); }

    /**
     *  The motivating case. A world-readable file asked to be user-only vetoes; under the old
     *  unchecked-fatal design this aborted MLog's static initializer and left the JVM with no
     *  logging at all, reported as a NoClassDefFoundError on every later touch.
     */
    public void testVetoingFileUrlInPathFileDoesNotPreventLogging() throws Exception
    {
        if ( !posixSupported() ) return;
        assertMLogComesUp( "vetoing file: URL",
                           rootNaming( "secret.key=exposed\n", "rw-r--r--", "permissions=useronly" ) );
    }

    /** A capitalized scheme must not slip past whatever handles the lowercase one. */
    public void testCapitalizedSchemeAlsoDoesNotPreventLogging() throws Exception
    {
        if ( !posixSupported() ) return;

        Path root    = Files.createDirectory( dir.resolve( "root-upper-" + System.nanoTime() ) );
        Path payload = dir.resolve( "payload-upper.properties" );
        Files.write( payload, "secret.key=exposed\n".getBytes( "8859_1" ) );
        Files.setPosixFilePermissions( payload, PosixFilePermissions.fromString( "rw-r--r--" ) );

        String lower = payload.toUri().toString() + "?permissions=useronly";
        String upper = "FILE:" + lower.substring( "file:".length() );
        Files.write( root.resolve( "mchange-config-resource-paths.txt" ), (upper + "\n").getBytes( "8859_1" ) );

        assertMLogComesUp( "capitalized FILE: URL", root.toFile() );
    }

    /** A required-but-absent file vetoes too, and must likewise not take logging down. */
    public void testRequiredButAbsentFileInPathFileDoesNotPreventLogging() throws Exception
    {
        Path root   = Files.createDirectory( dir.resolve( "root-absent-" + System.nanoTime() ) );
        Path absent = dir.resolve( "never-created.properties" );
        Files.write( root.resolve( "mchange-config-resource-paths.txt" ),
                     (absent.toUri().toString() + "?required=true\n").getBytes( "8859_1" ) );

        assertMLogComesUp( "required-but-absent file: URL", root.toFile() );
    }

    /** A malformed identifier in a path file is likewise survivable. */
    public void testMalformedIdentifierInPathFileDoesNotPreventLogging() throws Exception
    {
        Path root = Files.createDirectory( dir.resolve( "root-malformed-" + System.nanoTime() ) );
        Files.write( root.resolve( "mchange-config-resource-paths.txt" ),
                     "not-an-absolute-path.properties\n".getBytes( "8859_1" ) );

        assertMLogComesUp( "malformed identifier", root.toFile() );
    }
}
