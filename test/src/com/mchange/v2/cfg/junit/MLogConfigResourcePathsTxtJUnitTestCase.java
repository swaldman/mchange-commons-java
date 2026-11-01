package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 *  <code>mlog-config-resource-paths.txt</code>: how a library or application declares the
 *  resource-path list mlog should read.
 *
 *  <h3>Why this resource exists</h3>
 *
 *  <p>mlog used to hardcode c3p0's config sources -- <code>/c3p0.properties</code>,
 *  <code>/c3p0-default.properties</code>, the <code>c3p0-xml:</code> identifiers -- in its own
 *  source list, with a comment apologizing for it and asking that the two libraries' lists be
 *  kept in sync by hand. That is a layering inversion: conceptually c3p0 is just a prominent
 *  user of the logging library. Any jar on the classpath can now ship this resource to say where
 *  mlog's configuration lives, and c3p0 does, so mlog no longer names c3p0 anywhere.</p>
 *
 *  <h3>What is asserted, and why these particular things</h3>
 *
 *  <p>The discovered list <b>replaces</b> the hardcoded one rather than extending it, which is
 *  what makes the warnings about absent standard files worth having: a deployment that declares
 *  a list is taking responsibility for the whole of it, and silently losing
 *  <code>/mchange-commons.properties</code> would be a bad way to find that out.</p>
 *
 *  <p>The capping case is the one with teeth. A declared list that happens not to mention
 *  <code>/</code> or <code>SEALED_SYSTEM_PROPERTIES</code> must still let System properties win,
 *  or a <code>-D</code> would silently stop working for whoever wrote that file -- and "a sealed
 *  System property wins, full stop" is an invariant the rest of the library depends on.</p>
 *
 *  <p>The ordering and missing-file checks are table-driven over nine permutations because the
 *  predicate behind them is easy to get subtly wrong: a first implementation warned on canonical
 *  lists and stayed silent on reversed ones, and its replacement caught a full reversal while
 *  missing every partial misordering and warning spuriously on a one-entry list. Both passed the
 *  rest of the suite, since nothing counted these warnings.</p>
 *
 *  <h3>On the mechanics</h3>
 *
 *  <p>The resource is resolved through <code>MLogConfig.class.getClassLoader()</code>, so there
 *  is no injecting a loader from outside. CfgScenario is the existing answer: it loads the
 *  library afresh in a URLClassLoader whose first entries are directories this test creates, and
 *  MLogConfigDriver drives the resulting MLogConfig reflectively.</p>
 *
 *  <p>Warnings are asserted by presence rather than by count, deliberately.
 *  <code>bootstrapLogItems</code> accumulates across loads until something drains it, and MLog's
 *  own initialization performs a load before a test can install a recording logger -- so a count
 *  would depend on whether the MLog-Init-Reporter had got there first. Presence and absence are
 *  the properties that matter here and are not timing-dependent.</p>
 */
public final class MLogConfigResourcePathsTxtJUnitTestCase extends TestCase
{
    /** No resource-path text files, so without our resource the hardcoded branch is taken. */
    private final static String SCENARIO = "no-pathfiles";

    private final static String RESOURCE = "mlog-config-resource-paths.txt";

    private final static String STANDARD_DEFAULT_MCHANGE_LOG = "/com/mchange/v2/log/default-mchange-log.properties";
    private final static String STANDARD_MCHANGE_COMMONS     = "/mchange-commons.properties";
    private final static String STANDARD_MCHANGE_LOG         = "/mchange-log.properties";

    /** Set by the scenario's own /mchange-commons.properties, so it marks the hardcoded list. */
    private final static String HARDCODED_LIST_MARKER = "backstop.source";

    private final static String CONTESTED = "com.mchange.v2.cfg.junit.mlogpathstxt.contested";
    private final static String ONLY_ONE  = "com.mchange.v2.cfg.junit.mlogpathstxt.onlyOne";
    private final static String ONLY_TWO  = "com.mchange.v2.cfg.junit.mlogpathstxt.onlyTwo";

    private List<File>  roots;
    private Properties  savedSystemProperties;

    @Override
    protected void setUp()
    {
        roots = new ArrayList<File>();
        savedSystemProperties = (Properties) System.getProperties().clone();
    }

    @Override
    protected void tearDown()
    {
        System.setProperties( savedSystemProperties );
        for ( File root : roots ) deleteRecursively( root );
    }

    private static void deleteRecursively( File f )
    {
        File[] children = f.listFiles();
        if ( children != null ) for ( File child : children ) deleteRecursively( child );
        f.delete();
    }

    // ------------------------------------------------------------------ fixtures

    /**
     *  A classpath root carrying our resource with the given lines, plus two properties files
     *  the lines may name. Roots are returned in creation order, which is the order they go on
     *  the classpath -- so the first one made is the highest priority.
     */
    private File root( String... pathLines )
    {
        try
        {
            File dir = File.createTempFile( "mlogpathstxt-root", "" );
            dir.delete();
            dir.mkdirs();
            roots.add( dir );

            write( new File( dir, RESOURCE ), pathLines );
            write( new File( dir, "probe-one.properties" ), CONTESTED + "=from-one", ONLY_ONE + "=yes" );
            write( new File( dir, "probe-two.properties" ), CONTESTED + "=from-two", ONLY_TWO + "=yes" );
            return dir;
        }
        catch ( Exception e )
        { throw new RuntimeException( "Could not build a classpath root", e ); }
    }

    private static void write( File f, String... lines ) throws Exception
    {
        PrintWriter pw = new PrintWriter( f, "ISO-8859-1" );
        try { for ( String line : lines ) pw.println( line ); }
        finally { pw.close(); }
    }

    /** A scenario whose classpath begins with the given roots. */
    private CfgScenario scenario( File... extraRoots )
    { return CfgScenario.openWithRoots( SCENARIO, false, extraRoots ); }

    // ------------------------------------------------------------------ discovery

    /**
     *  The declared list is what gets read, and it REPLACES the hardcoded one.
     *
     *  <p>The second assertion is the load-bearing half. Asserting only that the declared
     *  resources were read would pass equally if mlog had read them in addition to its own
     *  defaults -- which is what a first version of this feature did, discovering the file and
     *  then reading the hardcoded list anyway.</p>
     */
    public void testTheDeclaredListIsReadAndReplacesTheHardcodedOne()
    {
        CfgScenario s = scenario( root( "/probe-one.properties", "/probe-two.properties" ) );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );

            assertEquals( "a key only the declared resources supply must be in force",
                          "yes", d.getProperty( ONLY_ONE ) );
            assertEquals( "yes", d.getProperty( ONLY_TWO ) );
            assertEquals( "and later entries in the declared list win",
                          "from-two", d.getProperty( CONTESTED ) );

            assertNull( "the hardcoded list must NOT also have been read -- the scenario's own " +
                        "/mchange-commons.properties sets this, and the declared list omits it",
                        d.getProperty( HARDCODED_LIST_MARKER ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** With no such resource anywhere, the accessor says so and the hardcoded list governs. */
    public void testWithNoResourceTheAccessorIsNullAndTheHardcodedListGoverns()
    {
        CfgScenario s = scenario();
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );

            assertNull( "no mlog-config-resource-paths.txt was found, which the accessor reports as null",
                        d.resourcePathsTxtPaths() );
            assertEquals( "so the hardcoded source list is in force",
                          "mchange-commons", d.getProperty( HARDCODED_LIST_MARKER ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** What was discovered is reportable, which is how a deployment can see what it declared. */
    public void testTheDiscoveredListIsReportedVerbatim()
    {
        CfgScenario s = scenario( root( "/probe-one.properties", "/probe-two.properties", "/" ) );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );

            assertEquals( Arrays.asList( "/probe-one.properties", "/probe-two.properties", "/" ),
                          d.resourcePathsTxtPaths() );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Blank lines, '#' comments and surrounding whitespace are ignored or trimmed. */
    public void testCommentsBlanksAndWhitespaceAreIgnored()
    {
        CfgScenario s = scenario( root( "# a comment",
                                        "",
                                        "   /probe-one.properties   ",
                                        "   ",
                                        "#/probe-two.properties",
                                        "/probe-two.properties" ) );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );

            assertEquals( "only the two real, trimmed paths should survive",
                          Arrays.asList( "/probe-one.properties", "/probe-two.properties" ),
                          d.resourcePathsTxtPaths() );
            assertEquals( "from-two", d.getProperty( CONTESTED ) );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------------ several copies

    /**
     *  The resource may be present on more than one classpath entry -- a framework jar and an
     *  application jar, say -- and then the EARLIER entry wins, per the JVM's usual convention
     *  for a contested resource.
     *
     *  <p>Which means the earlier entry's paths become the TAIL of the combined list, since
     *  within a resource-path list later paths take precedence. Both halves are asserted: the
     *  combined order, and that a key both lists' resources set resolves to the earlier
     *  entry's.</p>
     */
    public void testAnEarlierClasspathEntryWinsAndBecomesTheTail()
    {
        File first  = root( "/probe-one.properties" );   // earlier on the classpath
        File second = root( "/probe-two.properties" );

        CfgScenario s = scenario( first, second );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );

            assertEquals( "the earlier entry's paths belong at the end, where they win",
                          Arrays.asList( "/probe-two.properties", "/probe-one.properties" ),
                          d.resourcePathsTxtPaths() );
            assertEquals( "and so a contested key resolves to the earlier entry's resource",
                          "from-one", d.getProperty( CONTESTED ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Reversing which root comes first reverses both. The complement of the case above. */
    public void testTheOtherOrderReversesBoth()
    {
        File first  = root( "/probe-two.properties" );
        File second = root( "/probe-one.properties" );

        CfgScenario s = scenario( first, second );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );

            assertEquals( Arrays.asList( "/probe-one.properties", "/probe-two.properties" ),
                          d.resourcePathsTxtPaths() );
            assertEquals( "from-two", d.getProperty( CONTESTED ) );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------------ capping

    /**
     *  THE ONE WITH TEETH. A declared list that never mentions <code>/</code> or
     *  <code>SEALED_SYSTEM_PROPERTIES</code> must still give System properties top precedence.
     *
     *  <p>Otherwise whoever wrote the file would silently lose <code>-D</code> support, and
     *  "a sealed System property wins, full stop" is relied on elsewhere -- notably by the
     *  security controls, where an operator's refusal has to stand against anything a
     *  configuration file asks for. mlog therefore caps a declared list rather than trusting it
     *  to have capped itself.</p>
     */
    public void testASystemPropertyStillWinsOverADeclaredListThatOmitsIt()
    {
        System.setProperty( CONTESTED, "from-sysprop" );

        CfgScenario s = scenario( root( "/probe-one.properties", "/probe-two.properties" ) );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );

            assertFalse( "precondition: the declared list really does omit the System-property entries",
                         d.resourcePathsTxtPaths().contains( "/" ) );

            assertEquals( "a System property must outrank a declared list that forgot to say so",
                          "from-sysprop", d.getProperty( CONTESTED ) );
            assertEquals( "while keys the System properties do not claim still come from the list",
                          "yes", d.getProperty( ONLY_ONE ) );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------------ the advisory warnings

    private final static String MISSING_FRAGMENT  = "not found in the configured mlog path";
    private final static String ORDERING_FRAGMENT = "Ordering of standard mlog config files is non standard";

    /** Everything MLogConfig said during one scenario's load, rendered across the loader boundary. */
    private static String said( CfgScenario s )
    {
        MLogConfigDriver d = new MLogConfigDriver( s );
        d.initializeMLog();
        List<String[]> logged = d.installRecordingClassLogger();
        d.refresh( null, null );
        d.logDelayedItems();
        return MLogConfigDriver.describe( logged );
    }

    private String saidFor( String... pathLines )
    {
        CfgScenario s = scenario( root( pathLines ) );
        try { return said( s ); }
        finally { s.closeQuietly(); }
    }

    /**
     *  Each standard config source a declared list omits draws its own warning, and each one
     *  present draws none. The list replaces rather than extends, so an omission is a real loss
     *  and worth saying out loud -- separately per file, so the message names what to add.
     */
    public void testEachOmittedStandardSourceIsNamedAndEachPresentOneIsNot()
    {
        String allThree = saidFor( STANDARD_DEFAULT_MCHANGE_LOG, STANDARD_MCHANGE_COMMONS, STANDARD_MCHANGE_LOG );
        assertFalse( "a list naming all three standard sources should draw no missing-file warning: " + allThree,
                     allThree.contains( MISSING_FRAGMENT ) );

        String onlyCommons = saidFor( STANDARD_MCHANGE_COMMONS );
        assertTrue( "the absent default-mchange-log should be named: " + onlyCommons,
                    onlyCommons.contains( STANDARD_DEFAULT_MCHANGE_LOG ) );
        assertTrue( "and the absent mchange-log: " + onlyCommons,
                    onlyCommons.contains( STANDARD_MCHANGE_LOG ) );

        String none = saidFor( "/probe-one.properties" );
        assertTrue( "all three should be named when none is declared: " + none,
                    none.contains( STANDARD_DEFAULT_MCHANGE_LOG )
                    && none.contains( STANDARD_MCHANGE_COMMONS )
                    && none.contains( STANDARD_MCHANGE_LOG ) );
    }

    /**
     *  The canonical ordering of the standard sources draws no complaint, and every
     *  misordering does.
     *
     *  <p>Table-driven over the permutations that matter, because this predicate has been wrong
     *  twice. Reading <code>D</code>, <code>C</code>, <code>L</code> for the three standard
     *  sources in their recommended order: the first implementation's condition was true exactly
     *  when the order was canonical, so it warned about correct files and passed reversed ones.
     *  Its replacement caught <code>L,C,D</code> but not <code>D,L,C</code>, <code>C,D,L</code>
     *  or <code>L,C</code>, and warned spuriously about a list containing only <code>D</code> --
     *  because absent entries carry index -1 and a chained comparison over sentinels is not an
     *  ordering test. Each row below fails one of those two versions.</p>
     */
    public void testOnlyNonCanonicalOrderingIsReported()
    {
        String D = STANDARD_DEFAULT_MCHANGE_LOG, C = STANDARD_MCHANGE_COMMONS, L = STANDARD_MCHANGE_LOG;

        assertOrderingQuiet( "the canonical order", D, C, L );
        assertOrderingQuiet( "a single standard source cannot be misordered", D );
        assertOrderingQuiet( "nor can this one", C );
        assertOrderingQuiet( "nor this one", L );
        assertOrderingQuiet( "two in canonical relative order", D, C );
        assertOrderingQuiet( "a list naming none of them", "/probe-one.properties" );

        assertOrderingReported( "fully reversed", L, C, D );
        assertOrderingReported( "commons last", D, L, C );
        assertOrderingReported( "commons first", C, D, L );
        assertOrderingReported( "two, misordered", L, C );
        assertOrderingReported( "the outer pair misordered", L, D );
    }

    private void assertOrderingQuiet( String what, String... pathLines )
    {
        String said = saidFor( pathLines );
        assertFalse( what + " should draw no ordering warning: " + said,
                     said.contains( ORDERING_FRAGMENT ) );
    }

    private void assertOrderingReported( String what, String... pathLines )
    {
        String said = saidFor( pathLines );
        assertTrue( what + " should be reported: " + said,
                    said.contains( ORDERING_FRAGMENT ) );
    }
}
