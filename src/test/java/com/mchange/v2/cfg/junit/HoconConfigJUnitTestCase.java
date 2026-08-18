package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  HOCON-backed config paths, exercised with lightbend/typesafe config on the
 *  scenario classpath.
 *
 *  Several tests here mutate System properties that typesafe-config consults at parse
 *  time (config.resource / config.file / config.url). That is safe because the build
 *  sets Test / parallelExecution := false; each test restores what it changed.
 */
public final class HoconConfigJUnitTestCase extends TestCase
{
    private static List<String> readPaths( CfgScenario s, Object mpc )
    { return Arrays.asList( s.getPropertiesResourcePaths( mpc ) ); }

    // ------------------------------------------------------------ basics

    /** Later elements of a HOCON path override earlier ones. */
    public void testLaterHoconElementsOverrideEarlier()
    {
        CfgScenario s = CfgScenario.open( "hocon-basic" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/reference,/application" }, new ArrayList() );

            assertEquals( "from-application", s.getProperty( mpc, "hocon.key" ) );
            assertEquals( "yes", s.getProperty( mpc, "only.reference" ) );
            assertEquals( "yes", s.getProperty( mpc, "only.application" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Reversing the elements reverses precedence. */
    public void testHoconElementOrderIsSignificant()
    {
        CfgScenario s = CfgScenario.open( "hocon-basic" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/application,/reference" }, new ArrayList() );
            assertEquals( "from-reference", s.getProperty( mpc, "hocon.key" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** HOCON and plain properties paths layer together, later winning. */
    public void testHoconAndPlainPropertiesLayerTogether()
    {
        CfgScenario s = CfgScenario.open( "hocon-basic" );
        try
        {
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:/reference", "/plain.properties" }, new ArrayList() );

            assertEquals( "from-reference", s.getProperty( mpc, "hocon.key" ) );
            assertEquals( "plain-value",    s.getProperty( mpc, "plain.key" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  An element with no '.' is read with all three HOCON suffixes. The MConfig javadoc
     *  states the order as .properties, then .json, then .conf, with later winning.
     */
    public void testSuffixlessElementReadsAllThreeSyntaxes()
    {
        CfgScenario s = CfgScenario.open( "hocon-suffixes" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/multi" }, new ArrayList() );

            // deliberately dot-free keys: a quoted dotted key in JSON is a literal key,
            // not a path, which would make this fixture test HOCON quoting rather than
            // suffix expansion
            assertEquals( ".conf should have been read",       "yes", s.getProperty( mpc, "seenConf" ) );
            assertEquals( ".json should have been read",       "yes", s.getProperty( mpc, "seenJson" ) );
            assertEquals( ".properties should have been read", "yes", s.getProperty( mpc, "seenProperties" ) );

            assertEquals( ".conf should take preference over .json and .properties",
                          "conf", s.getProperty( mpc, "which" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** A '#scope' suffix re-roots the config at that subtree. */
    public void testScopeSuffixRerootsConfig()
    {
        CfgScenario s = CfgScenario.open( "hocon-scope" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/scoped.conf#my-scope" }, new ArrayList() );

            assertEquals( "apple", s.getProperty( mpc, "a" ) );
            assertEquals( "book",  s.getProperty( mpc, "b" ) );
            assertNull( "keys outside the scope should not be exposed",
                        s.getProperty( mpc, "some-top-level-key" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Without the scope suffix, the whole file is visible and nothing is re-rooted. */
    public void testWithoutScopeSuffixWholeFileIsVisible()
    {
        CfgScenario s = CfgScenario.open( "hocon-scope" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/scoped.conf" }, new ArrayList() );

            assertEquals( "hello", s.getProperty( mpc, "some-top-level-key" ) );
            assertEquals( "apple", s.getProperty( mpc, "my-scope.a" ) );
            assertNull( s.getProperty( mpc, "a" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Substitutions within the merged config are resolved. */
    public void testSubstitutionsAreResolved()
    {
        CfgScenario s = CfgScenario.open( "hocon-substitution" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/reference" }, new ArrayList() );
            assertEquals( "/base/sub", s.getProperty( mpc, "derived.path" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** The special element "/" contributes System properties to the merged config. */
    public void testSlashElementContributesSystemProperties()
    {
        CfgScenario s = CfgScenario.open( "hocon-basic" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/reference,/" }, new ArrayList() );
            assertEquals( System.getProperty( "user.home" ), s.getProperty( mpc, "user.home" ) );
            assertEquals( "from-reference", s.getProperty( mpc, "hocon.key" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------- interresolvability of overlaps

    /**
     *  Two HOCON paths sharing an element fall back to one another, so a substitution
     *  that only one of them satisfies still resolves for both.
     *
     *  common.conf needs ${who}; only app1.conf supplies it. Without overlap handling,
     *  "hocon:/common,/app2" could not resolve and would be dropped.
     */
    public void testOverlappingHoconPathsResolveEachOthersSubstitutions()
    {
        CfgScenario s = CfgScenario.open( "hocon-overlap" );
        try
        {
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:/common,/app1", "hocon:/common,/app2" }, new ArrayList() );

            assertEquals( "both overlapping paths should have resolved",
                          2, readPaths( s, mpc ).size() );
            assertEquals( "hello world", s.getProperty( mpc, "greeting" ) );
            assertEquals( "the later path's own elements should still win",
                          "two", s.getProperty( mpc, "app" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** A path whose substitution nothing satisfies fails cleanly, on its own. */
    public void testUnsatisfiableSubstitutionIsReportedNotThrown()
    {
        CfgScenario s = CfgScenario.open( "hocon-overlap" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/needsname" }, new ArrayList() );

            assertEquals( "the unresolvable path should have been dropped",
                          0, readPaths( s, mpc ).size() );
            assertNull( s.getProperty( mpc, "msg" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  Overlap detection must be symmetric across suffix expansion. Here the path with
     *  the BARE element ("hocon:/needsname") is the one that needs help, and the path
     *  offering it names the same resource with an explicit suffix
     *  ("hocon:/needsname.conf,/namer").
     *
     *  Pass 1 registers /needsname's expansions, but pass 2 once looked up only the bare
     *  element, so the bare path could not see the suffixed one and failed to resolve
     *  while the suffixed path resolved fine. Both directions must work.
     */
    public void testOverlapDetectionIsSymmetricAcrossSuffixExpansion()
    {
        CfgScenario s = CfgScenario.open( "hocon-overlap" );
        try
        {
            List out = new ArrayList();
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:/needsname", "hocon:/needsname.conf,/namer" }, out );

            List<String[]> items = s.getDelayedLogItems( mpc );
            assertEquals( "both paths should have resolved, got:" + CfgScenario.describe( items ),
                          2, readPaths( s, mpc ).size() );
            assertEquals( "hi bob", s.getProperty( mpc, "msg" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** And the same pair in the other order. */
    public void testOverlapDetectionIsSymmetricInEitherOrder()
    {
        CfgScenario s = CfgScenario.open( "hocon-overlap" );
        try
        {
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:/needsname.conf,/namer", "hocon:/needsname" }, new ArrayList() );

            assertEquals( 2, readPaths( s, mpc ).size() );
            assertEquals( "hi bob", s.getProperty( mpc, "msg" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Sharing only "/" is not overlap: System properties carry no substitutions. */
    public void testSharedSystemPropertiesElementIsNotOverlap()
    {
        CfgScenario s = CfgScenario.open( "hocon-overlap" );
        try
        {
            // /app1 and /app2 share only "/", so /needsname gets no help and drops out
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:/app1,/", "hocon:/needsname,/" }, new ArrayList() );

            assertEquals( "one", s.getProperty( mpc, "app" ) );
            assertNull( "the unresolvable path should not have been rescued via '/'",
                        s.getProperty( mpc, "msg" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------- application overrides

    /** config.resource redirects the special 'application' element. */
    public void testConfigResourceOverridesApplication()
    {
        String saved = System.getProperty( "config.resource" );
        CfgScenario s = CfgScenario.open( "hocon-config-override" );
        try
        {
            System.setProperty( "config.resource", "alt.conf" );
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/application" }, new ArrayList() );
            assertEquals( "alt-resource", s.getProperty( mpc, "override.source" ) );
        }
        finally
        {
            restore( "config.resource", saved );
            s.closeQuietly();
        }
    }

    /** With nothing set, the ordinary application.conf is used. */
    public void testApplicationUsedWhenNoOverrideSet()
    {
        CfgScenario s = CfgScenario.open( "hocon-config-override" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/application" }, new ArrayList() );
            assertEquals( "application", s.getProperty( mpc, "override.source" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** config.file redirects the special 'application' element to a filesystem file. */
    public void testConfigFileOverridesApplication() throws Exception
    {
        String saved = System.getProperty( "config.file" );
        File f = File.createTempFile( "cfgtest-", ".conf" );
        CfgScenario s = CfgScenario.open( "hocon-config-override" );
        try
        {
            FileWriter w = new FileWriter( f );
            try { w.write( "override.source = from-file\n" ); }
            finally { w.close(); }

            System.setProperty( "config.file", f.getAbsolutePath() );
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/application" }, new ArrayList() );
            assertEquals( "from-file", s.getProperty( mpc, "override.source" ) );
        }
        finally
        {
            restore( "config.file", saved );
            f.delete();
            s.closeQuietly();
        }
    }

    /** config.url likewise. */
    public void testConfigUrlOverridesApplication() throws Exception
    {
        String saved = System.getProperty( "config.url" );
        File f = File.createTempFile( "cfgtest-", ".conf" );
        CfgScenario s = CfgScenario.open( "hocon-config-override" );
        try
        {
            FileWriter w = new FileWriter( f );
            try { w.write( "override.source = from-url\n" ); }
            finally { w.close(); }

            System.setProperty( "config.url", f.toURI().toURL().toExternalForm() );
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/application" }, new ArrayList() );
            assertEquals( "from-url", s.getProperty( mpc, "override.source" ) );
        }
        finally
        {
            restore( "config.url", saved );
            f.delete();
            s.closeQuietly();
        }
    }

    /** config.resource is checked before config.file. */
    public void testConfigResourceTakesPrecedenceOverConfigFile() throws Exception
    {
        String savedRsrc = System.getProperty( "config.resource" );
        String savedFile = System.getProperty( "config.file" );
        File f = File.createTempFile( "cfgtest-", ".conf" );
        CfgScenario s = CfgScenario.open( "hocon-config-override" );
        try
        {
            FileWriter w = new FileWriter( f );
            try { w.write( "override.source = from-file\n" ); }
            finally { w.close(); }

            System.setProperty( "config.resource", "alt.conf" );
            System.setProperty( "config.file", f.getAbsolutePath() );

            Object mpc = s.asProvidedUncached( new String[] { "hocon:/application" }, new ArrayList() );
            assertEquals( "alt-resource", s.getProperty( mpc, "override.source" ) );
        }
        finally
        {
            restore( "config.resource", savedRsrc );
            restore( "config.file", savedFile );
            f.delete();
            s.closeQuietly();
        }
    }

    /** A config.file that does not exist warns and falls back to application.conf. */
    public void testMissingConfigFileWarnsAndFallsBack()
    {
        String saved = System.getProperty( "config.file" );
        CfgScenario s = CfgScenario.open( "hocon-config-override" );
        try
        {
            System.setProperty( "config.file", "/no/such/path/nowhere.conf" );
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/application" }, new ArrayList() );

            assertEquals( "should fall back to the standard application resource",
                          "application", s.getProperty( mpc, "override.source" ) );

            List<String[]> items = s.getDelayedLogItems( mpc );
            assertTrue( "expected a WARNING about the missing config.file, got:" + CfgScenario.describe( items ),
                        CfgScenario.hasLogItem( items, "WARNING", "does not exist" ) );
        }
        finally
        {
            restore( "config.file", saved );
            s.closeQuietly();
        }
    }

    private static void restore( String key, String saved )
    {
        if ( saved == null ) System.clearProperty( key );
        else                 System.setProperty( key, saved );
    }
}
