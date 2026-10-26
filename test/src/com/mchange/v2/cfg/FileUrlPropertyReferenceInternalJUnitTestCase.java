package com.mchange.v2.cfg;

import junit.framework.TestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 *  <code>file:${some.property}</code>: a resource identifier that names a System property holding
 *  the path, rather than carrying the path itself.
 *
 *  <h3>Why the feature exists</h3>
 *
 *  <p>A hardcoded source list has to be able to name a file whose location only a deployment
 *  knows. c3p0 does that today for <code>com.mchange.v2.c3p0.cfg.xml</code> by reading the
 *  property itself, inside its config finder -- which works only because c3p0 interprets its own
 *  sources. A library that just lists resource identifiers has nowhere to do that, so the
 *  identifier has to say it.</p>
 *
 *  <h3>Why this is an internal test</h3>
 *
 *  <p>References resolve against the SEALED snapshot of System properties, never the live ones.
 *  That is deliberate -- it matches how the properties naming config files have always been read,
 *  and MConfig's cache is keyed on the identifier string, so an identifier whose meaning could
 *  drift would let the cache return content from a file it no longer points at.</p>
 *
 *  <p>The consequence for testing is sharp, and was not obvious until the first attempt at these
 *  cases failed wholesale: since MLogConfig now caps on every load, MLog's own initialization
 *  seals the snapshot. By the time any test method runs, the snapshot is taken, and a property set
 *  afterwards is invisible to it. So these cases must discard the snapshot first, which needs
 *  SecurityRatchetTestSupport and therefore this package.</p>
 *
 *  <p>Worth stating plainly, because it is the deployment story too: a <code>${...}</code>
 *  reference resolves to what the property held at the seal -- in practice, to a
 *  <code>-D</code> on the command line. Setting the property from application code after startup
 *  will not redirect a config read.</p>
 */
public final class FileUrlPropertyReferenceInternalJUnitTestCase extends TestCase
{
    private final static String[] NONE = new String[0];

    private final static String PFX = "com.mchange.v2.cfg.fileurlref.";

    private Path dir;
    private Path userOnly;   // 0600  secret.key=from-useronly
    private Path worldRead;  // 0644  secret.key=from-worldread

    private Properties saved;

    @Override
    protected void setUp() throws Exception
    {
        // toRealPath: on macOS the temp dir sits under /var, itself a symlink, and the permissions
        // chain reasons about links -- we want the paths we think we have
        dir       = Files.createTempDirectory( "mchange-cfg-fileurlref-" ).toRealPath();
        userOnly  = write( "useronly.properties",  "secret.key=from-useronly\n", "rw-------" );
        worldRead = write( "worldread.properties", "secret.key=from-worldread\n", "rw-r--r--" );

        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
        SecurityRatchetTestSupport.unsealSystemProperties();
    }

    @Override
    protected void tearDown() throws Exception
    {
        deleteRecursively( dir );
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( k.startsWith( PFX ) ) System.setProperty( k, saved.getProperty( k ) );
        SecurityRatchetTestSupport.unsealSystemProperties();
    }

    // ------------------------------------------------------------- fixture

    private Path write( String name, String contents, String mode ) throws IOException
    {
        Path p = dir.resolve( name );
        Files.write( p, contents.getBytes( "8859_1" ) );
        if ( posixSupported() ) Files.setPosixFilePermissions( p, PosixFilePermissions.fromString( mode ) );
        return p;
    }

    private boolean posixSupported()
    { return dir.getFileSystem().supportedFileAttributeViews().contains( "posix" ); }

    private static void deleteRecursively( Path p )
    {
        try
        {
            if ( Files.isDirectory( p ) )
                for ( Path child : Files.newDirectoryStream( p ) ) deleteRecursively( child );
            Files.deleteIfExists( p );
        }
        catch ( Exception e ) { /* best effort */ }
    }

    private static void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( k.startsWith( PFX ) ) doomed.add( k );
        for ( String k : doomed ) System.clearProperty( k );
    }

    private static void setRef( String suffix, String value )
    { System.setProperty( PFX + suffix, value ); }

    private static String ref( String suffix )
    { return "${" + PFX + suffix + "}"; }

    /** An identifier whose path is entirely one reference. */
    private static String refUrl( String suffix, String query )
    { return "file:" + ref( suffix ) + (query == null ? "" : "?" + query); }

    private static String refUrl( String suffix )
    { return refUrl( suffix, null ); }

    /** file: sources are vetoable, so reads go through the vetoable facade. */
    @SuppressWarnings("unchecked")
    private static MultiPropertiesConfig read( String... paths ) throws ConfigVetoedException
    { return MConfig.AsProvidedVetoable.readUncachedResourceConfig( NONE, paths, new ArrayList() ); }

    private static List<String> pathsOf( MultiPropertiesConfig mpc )
    { return Arrays.asList( mpc.getPropertiesResourcePaths() ); }

    @SuppressWarnings("unchecked")
    private static boolean hasFineSkip( MultiPropertiesConfig mpc )
    {
        for ( Object o : mpc.getDelayedLogItems() )
        {
            DelayedLogItem item = (DelayedLogItem) o;
            if ( DelayedLogItem.Level.FINE.equals( item.getLevel() )
                 && item.getText() != null && item.getText().contains( "could not be found. Skipping." ) )
                return true;
        }
        return false;
    }

    /**
     *  A source that cannot be parsed is REPORTED, not thrown: BasicMultiPropertiesConfig turns a
     *  ConfigParseException into a delayed item and the source simply contributes nothing. Only a
     *  veto -- a refusal on security grounds -- propagates to the caller. So a malformed
     *  identifier is checked by what it says and what it contributed, never by a catch.
     */
    @SuppressWarnings("unchecked")
    private static boolean hasWarningContaining( MultiPropertiesConfig mpc, String fragment )
    {
        for ( Object o : mpc.getDelayedLogItems() )
        {
            DelayedLogItem item = (DelayedLogItem) o;
            if ( DelayedLogItem.Level.WARNING.equals( item.getLevel() )
                 && item.getText() != null && item.getText().contains( fragment ) )
                return true;
        }
        return false;
    }

    private static String assertVetoed( String... paths )
    {
        try
        {
            MultiPropertiesConfig mpc = read( paths );
            fail( "expected a veto, but the read succeeded with " + pathsOf( mpc ) );
            return null; // unreachable
        }
        catch ( ConfigVetoedException expected )
        { return expected.getMessage(); }
    }

    // =================================================== the three shapes

    public void testAWholePathReferenceLoads() throws Exception
    {
        setRef( "whole", userOnly.toString() );

        assertEquals( "from-useronly", read( refUrl( "whole" ) ).getProperty( "secret.key" ) );
    }

    /** The empty-authority spelling, which is how a file URL is usually written. */
    public void testTheDoubleSlashSpellingLoadsToo() throws Exception
    {
        setRef( "dbl", userOnly.toString() );

        assertEquals( "from-useronly", read( "file://" + ref( "dbl" ) ).getProperty( "secret.key" ) );
    }

    /**
     *  Literal text after the reference, which is the shape the single-view C3P0Config work wants:
     *  a deployment names a directory, the library names the file within it.
     */
    public void testAReferenceMayCarryLiteralTextAfterIt() throws Exception
    {
        setRef( "dir", dir.toString() );

        assertEquals( "from-useronly",
                      read( "file:" + ref( "dir" ) + "/useronly.properties" ).getProperty( "secret.key" ) );
    }

    public void testSeveralReferencesInOneIdentifier() throws Exception
    {
        setRef( "d", dir.toString() );
        setRef( "base", "useronly" );

        assertEquals( "from-useronly",
                      read( "file:" + ref( "d" ) + "/" + ref( "base" ) + ".properties" ).getProperty( "secret.key" ) );
    }

    // =================================================== sealed, not live

    /**
     *  THE DECISION WITH NO OTHER WITNESS. A reference resolves against the snapshot, so changing
     *  the property afterwards cannot redirect the read. Were this live, MConfig's identifier-keyed
     *  cache would be free to return content from a file the identifier no longer names.
     */
    public void testAReferenceResolvesAgainstTheSnapshotNotTheLiveProperty() throws Exception
    {
        Path second = write( "second.properties", "secret.key=from-second\n", "rw-------" );

        setRef( "pinned", userOnly.toString() );
        assertEquals( "precondition: the first read seals with the first value",
                      "from-useronly", read( refUrl( "pinned" ) ).getProperty( "secret.key" ) );

        setRef( "pinned", second.toString() );
        assertEquals( "a later write to the property must not redirect the read",
                      "from-useronly", read( refUrl( "pinned" ) ).getProperty( "secret.key" ) );
    }

    // =================================================== what gets reported

    /**
     *  THE DECISION MOST LIKELY TO BE BROKEN BY A LATER REFACTOR. What is reported -- in source
     *  lists, in config dumps, and as the key a caller looks a source up by -- is the identifier
     *  AS WRITTEN. Stable across machines, matching the cache key, and keeping filesystem layout
     *  out of diagnostics.
     */
    public void testTheReportedResourcePathIsTheIdentifierAsWritten() throws Exception
    {
        setRef( "reported", userOnly.toString() );
        String identifier = refUrl( "reported" );

        MultiPropertiesConfig mpc = read( identifier );

        assertEquals( Arrays.asList( identifier ), pathsOf( mpc ) );
        assertFalse( "a config dump must not carry the filesystem path: " + pathsOf( mpc ),
                     pathsOf( mpc ).toString().contains( userOnly.toString() ) );
        assertEquals( "and a source is looked up by the identifier, not by what it resolved to",
                      "from-useronly",
                      mpc.getPropertiesByResourcePath( identifier ).getProperty( "secret.key" ) );
    }

    /** The resolution is still discoverable, at FINE, for whoever is debugging a path. */
    @SuppressWarnings("unchecked")
    public void testTheResolutionIsReportedAtFine() throws Exception
    {
        setRef( "fine", userOnly.toString() );

        MultiPropertiesConfig mpc = read( refUrl( "fine" ) );

        boolean found = false;
        for ( Object o : mpc.getDelayedLogItems() )
        {
            DelayedLogItem item = (DelayedLogItem) o;
            if ( DelayedLogItem.Level.FINE.equals( item.getLevel() )
                 && item.getText() != null
                 && item.getText().contains( "resolved to path" )
                 && item.getText().contains( userOnly.toString() ) ) found = true;
        }
        assertTrue( "expected a FINE item naming the resolved path", found );
    }

    // =================================================== an unset property is an absent file

    /**
     *  An unset property is treated as an absent file, which is why no new query key was needed:
     *  required= already says what to do about absence. The default is the ordinary FINE skip...
     */
    public void testAnUnsetPropertyIsSkippedLikeAMissingFile() throws Exception
    {
        MultiPropertiesConfig mpc = read( refUrl( "never-set" ) );

        assertNull( "nothing should have been read", mpc.getProperty( "secret.key" ) );
        assertTrue( "expected the ordinary FINE skip notice", hasFineSkip( mpc ) );
    }

    /** ...and required=true makes it fatal, so required= now means "set AND present". */
    public void testAnUnsetPropertyIsFatalWhenRequired()
    {
        String msg = assertVetoed( refUrl( "never-set", "required=true" ) );

        assertTrue( "the veto should name the identifier: " + msg, msg.contains( "never-set" ) );
    }

    /** A property set to whitespace is no more a path than an unset one. */
    public void testABlankPropertyCountsAsUnset() throws Exception
    {
        setRef( "blank", "   " );

        assertTrue( hasFineSkip( read( refUrl( "blank" ) ) ) );
    }

    // =================================================== malformed references

    public void testAnUnclosedReferenceIsRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( "file:${" + PFX + "unclosed" );

        assertEquals( "the source must contribute nothing", Arrays.asList(), pathsOf( mpc ) );
        assertTrue( "and must name the identifier it could not parse",
                    hasWarningContaining( mpc, "unclosed" ) );
        assertTrue( "and say what was wrong with it",
                    hasWarningContaining( mpc, "never closed" ) );
    }

    public void testAnEmptyReferenceIsRefused() throws Exception
    {
        MultiPropertiesConfig mpc = read( "file:${}" );

        assertEquals( "the source must contribute nothing", Arrays.asList(), pathsOf( mpc ) );
        assertTrue( "and must say why", hasWarningContaining( mpc, "names no System property" ) );
    }

    /** A property value that is not an absolute path hits the existing absolute-path rule. */
    public void testARelativeResolutionIsRefused() throws Exception
    {
        setRef( "relative", "etc/not-absolute.properties" );

        MultiPropertiesConfig mpc = read( refUrl( "relative" ) );

        assertEquals( "the source must contribute nothing", Arrays.asList(), pathsOf( mpc ) );
        assertTrue( "the existing absolute-path rule should be what refuses it",
                    hasWarningContaining( mpc, "absolute" ) );
    }

    /** No re-expansion: a value containing ${...} is a path, not another reference. */
    public void testAValueIsNotItselfExpanded() throws Exception
    {
        setRef( "inner", userOnly.toString() );
        setRef( "outer", "${" + PFX + "inner}" );

        MultiPropertiesConfig mpc = read( refUrl( "outer" ) );

        // the outer value is appended verbatim, so the path is the literal "${...inner}" -- which
        // is relative, and so refused by the absolute-path rule. No expansion loop can be
        // constructed, and no property named indirectly through a value.
        assertNull( "the inner reference must not have been followed", mpc.getProperty( "secret.key" ) );
        assertTrue( "the literal reference text should be what was treated as a path",
                    hasWarningContaining( mpc, PFX + "inner" ) );
    }

    // =================================================== the rest of the machinery still applies

    /** Query directives are parsed before substitution, and still mean what they mean. */
    public void testPermissionsDirectivesStillApplyAcrossSubstitution() throws Exception
    {
        if ( !posixSupported() ) return;

        setRef( "secure", userOnly.toString() );
        setRef( "insecure", worldRead.toString() );

        assertEquals( "a 0600 file still passes useronly",
                      "from-useronly",
                      read( refUrl( "secure", "permissions=useronly" ) ).getProperty( "secret.key" ) );

        String msg = assertVetoed( refUrl( "insecure", "permissions=useronly" ) );
        assertTrue( "the veto should still be about permissions: " + msg, msg.contains( "permissions" ) );
    }

    /**
     *  The query string is split off BEFORE substitution, so a '?' inside a property value is part
     *  of the path rather than a query delimiter. '?' is a legal filename character on Unix, and
     *  since unrecognized query keys are vetoed, reinterpreting the tail of such a path would
     *  surface as a confusing refusal rather than as a missing file.
     */
    public void testAQuestionMarkInAPropertyValueIsPartOfThePath() throws Exception
    {
        if ( !posixSupported() ) return; // the filename is not portable

        Path odd = write( "has?question.properties", "secret.key=from-odd\n", "rw-------" );
        setRef( "odd", odd.toString() );

        assertEquals( "from-odd", read( refUrl( "odd" ) ).getProperty( "secret.key" ) );
    }

    // =================================================== the percent-decoding divergence

    /**
     *  THE DIVERGENCE WE ACCEPTED, and the half that lives here. A referenced identifier is a
     *  filesystem path, not a URL, and is NOT percent-decoded: a System property holding a path
     *  carries no escapes, and a path containing a space could not survive new URI(..) at all.
     *
     *  <p>The other half -- that a plain file: identifier IS decoded -- is pinned in
     *  FileUrlConfigJUnitTestCase.testAPlainFileUrlIsPercentDecoded, so the divergence cannot
     *  drift from either side.</p>
     */
    public void testAReferencedIdentifierIsNotPercentDecoded() throws Exception
    {
        Path spaced = write( "has space.properties", "secret.key=from-spaced\n", "rw-------" );

        // the raw path, spaces and all, is exactly what a -D would carry -- and it works, which
        // is the half that could not work through new URI(..)
        setRef( "spaced", spaced.toString() );
        assertEquals( "from-spaced", read( refUrl( "spaced" ) ).getProperty( "secret.key" ) );

        // while a %-escaped spelling is taken literally, and so finds nothing
        setRef( "escaped", dir.resolve( "has%20space.properties" ).toString() );
        assertTrue( "a %-escape in a referenced path is a literal, not a space",
                    hasFineSkip( read( refUrl( "escaped" ) ) ) );
    }
}
