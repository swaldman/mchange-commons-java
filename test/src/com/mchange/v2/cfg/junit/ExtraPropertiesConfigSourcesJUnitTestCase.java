package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  <code>mcfg-extra-properties-config-sources.txt</code>: how a library teaches this one to read
 *  an identifier form it has never heard of.
 *
 *  <h3>Why the extension point exists</h3>
 *
 *  <p><code>ConfigUtils.propertiesConfigSourceForIdentifier</code> carried a standing note that
 *  it "should eventually be extensible via some public API". It hardcoded every form it knew,
 *  which meant that supporting c3p0's XML put c3p0's file format <i>inside this library</i> --
 *  a layering inversion apologized for in a comment and kept honest by hand. A jar may now ship
 *  this resource naming {@link com.mchange.v2.cfg.ExtraPropertiesConfigSources} implementations,
 *  each consulted for an identifier nothing built in claims. c3p0 ships one for
 *  <code>c3p0-xml:</code>, and this library no longer mentions c3p0 anywhere.</p>
 *
 *  <h3>The two orderings, which are opposite</h3>
 *
 *  <p>Resolvers are <b>first-found</b>: the earliest declaration wins, across files by classpath
 *  order and within a file by line order, because the first resolver that claims an identifier
 *  decides how it is read. Resource <i>paths</i> are the other way round -- later paths take
 *  precedence, because the last value written wins. Both are asserted here, since a reader who
 *  knows one rule will guess the other wrongly.</p>
 *
 *  <h3>On the mechanics</h3>
 *
 *  <p>The resource is resolved through <code>ExtraPropertiesConfigSources.class.getClassLoader()</code>,
 *  so a test cannot inject a loader. {@link CfgScenario} is the existing answer: it loads this
 *  library afresh in a URLClassLoader over a classpath the test builds. Two consequences are
 *  load-bearing here. The resolver classes must come from that same loader -- otherwise the
 *  <code>ExtraPropertiesConfigSources</code> they extend is a different class and they will not
 *  cast -- so this test's own code source goes on the scenario classpath, which is what
 *  {@link #testClassesRoot} is for. And the resolver list is cached in a static, so each case
 *  needs its own loader to get a fresh cache; that is what a per-case scenario gives.</p>
 */
public final class ExtraPropertiesConfigSourcesJUnitTestCase extends TestCase
{
    private final static String RESOURCE = "mcfg-extra-properties-config-sources.txt";

    private final static String PKG   = "com.mchange.v2.cfg.junit.extras.";
    private final static String ALPHA = PKG + "AlphaExtraPropertiesConfigSources";
    private final static String BETA  = PKG + "BetaExtraPropertiesConfigSources";
    private final static String BOTH  = PKG + "BothSchemesExtraPropertiesConfigSources";
    private final static String VETO  = PKG + "VetoableExtraPropertiesConfigSources";

    private final static String WHO_KEY = "com.mchange.v2.cfg.junit.extras.whoAnswered";

    /** No resource-path text files, so nothing the scenario ships competes with what we declare. */
    private final static String SCENARIO = "no-pathfiles";

    private List<File> roots;

    @Override
    protected void setUp()
    { roots = new ArrayList<File>(); }

    @Override
    protected void tearDown()
    { for ( File root : roots ) deleteRecursively( root ); }

    private static void deleteRecursively( File f )
    {
        File[] children = f.listFiles();
        if ( children != null ) for ( File child : children ) deleteRecursively( child );
        f.delete();
    }

    // ------------------------------------------------------------------ fixtures

    /** A classpath root carrying the extras resource with the given lines. */
    private File root( String... lines )
    {
        try
        {
            File dir = File.createTempFile( "extrasrc-root", "" );
            dir.delete();
            dir.mkdirs();
            roots.add( dir );
            write( new File( dir, RESOURCE ), lines );
            return dir;
        }
        catch ( Exception e )
        { throw new RuntimeException( "Could not build a classpath root", e ); }
    }

    private static void write( File f, String... lines ) throws Exception
    {
        PrintWriter pw = new PrintWriter( f, "UTF-8" );
        try { for ( String line : lines ) pw.println( line ); }
        finally { pw.close(); }
    }

    /**
     *  Where this test's classes live, so the scenario loader can define the resolver classes
     *  itself. Without this they would be loaded by the application loader, and the
     *  ExtraPropertiesConfigSources they extend would not be the scenario's -- the instantiation
     *  would fail its cast and every case would see "no resolver claimed this".
     */
    private static File testClassesRoot()
    {
        URL loc = ExtraPropertiesConfigSourcesJUnitTestCase.class.getProtectionDomain().getCodeSource().getLocation();
        try { return new File( loc.toURI() ); }
        catch ( Exception e )
        { throw new RuntimeException( "Could not locate this test's own classes at " + loc, e ); }
    }

    /** A scenario over the given extras roots, with this test's classes available to it. */
    private CfgScenario scenario( File... extrasRoots )
    {
        List<File> all = new ArrayList<File>( Arrays.asList( extrasRoots ) );
        all.add( testClassesRoot() );
        return CfgScenario.openWithRoots( SCENARIO, false, all.toArray( new File[ all.size() ] ) );
    }

    @SuppressWarnings("unchecked")
    private static List<String[]> read( CfgScenario s, String identifier, String[] holder )
    {
        List items = new ArrayList();
        Object mpc = s.asProvidedUncached( new String[] { identifier }, items );
        holder[0]  = s.getProperty( mpc, WHO_KEY );
        return s.renderLogItems( items );
    }

    private static boolean says( List<String[]> items, String level, String fragment )
    {
        for ( String[] item : items )
            if ( item[0] != null && item[0].contains( level )
                 && item[1] != null && item[1].contains( fragment ) ) return true;
        return false;
    }

    private static String describe( List<String[]> items )
    {
        StringBuilder sb = new StringBuilder();
        for ( String[] item : items ) sb.append( "\n    [" ).append( item[0] ).append( "] " ).append( item[1] );
        return sb.length() == 0 ? "(nothing reported)" : sb.toString();
    }

    // ------------------------------------------------------------------ the basics

    /** A declared resolver is consulted, and its source's properties arrive in the config. */
    public void testADeclaredResolverHandlesItsOwnScheme()
    {
        CfgScenario s = scenario( root( ALPHA ) );
        try
        {
            String[] who = new String[1];
            List<String[]> items = read( s, "alpha:something", who );

            assertEquals( "the declared resolver should have answered: " + describe( items ), ALPHA, who[0] );
        }
        finally
        { s.closeQuietly(); }
    }

    /** An identifier no declared resolver claims is reported, and is not fatal. */
    public void testAnUnclaimedIdentifierIsReported()
    {
        CfgScenario s = scenario( root( ALPHA ) );
        try
        {
            String[] who = new String[1];
            List<String[]> items = read( s, "gamma:nobody-claims-this", who );

            assertNull( "nothing should have answered", who[0] );
            assertTrue( "and the identifier should be named at WARNING: " + describe( items ),
                        says( items, "WARNING", "gamma:nobody-claims-this" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  And with NO extras resource anywhere, an unclaimed identifier is reported the same way.
     *
     *  <p>Not an obvious case to cover, which is why it is here: an earlier version put this
     *  warning inside the "did we find any resolvers" branch, so the diagnostic depended on
     *  whether some unrelated jar happened to ship the resource. Same typo, loud or silent
     *  according to the classpath.</p>
     */
    public void testAnUnclaimedIdentifierIsReportedEvenWithNoExtrasAtAll()
    {
        CfgScenario s = scenario();
        try
        {
            String[] who = new String[1];
            List<String[]> items = read( s, "gamma:nobody-claims-this", who );

            assertNull( who[0] );
            assertTrue( "the warning must not depend on some other jar shipping the resource: " + describe( items ),
                        says( items, "WARNING", "gamma:nobody-claims-this" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Built-in forms still win: a resolver claiming everything cannot intercept them. */
    public void testADeclaredResolverCannotInterceptABuiltInForm()
    {
        CfgScenario s = scenario( root( BOTH ) );
        try
        {
            String[] who = new String[1];
            read( s, "/com/mchange/v2/cfg/junit/a.properties", who );

            assertNull( "a resolver claiming every identifier must not get the built-in forms", who[0] );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------------ first-found ordering

    /**
     *  Within one file, the earlier line wins. Both resolvers here claim the identifier, so the
     *  answer names which one was actually consulted.
     */
    public void testWithinOneFileTheEarlierLineWins()
    {
        CfgScenario s = scenario( root( ALPHA, BOTH ) );
        try
        {
            String[] who = new String[1];
            read( s, "alpha:contested", who );
            assertEquals( ALPHA, who[0] );
        }
        finally
        { s.closeQuietly(); }
    }

    /** The complement: reverse the lines and the other resolver answers. */
    public void testReversingTheLinesReversesWhoAnswers()
    {
        CfgScenario s = scenario( root( BOTH, ALPHA ) );
        try
        {
            String[] who = new String[1];
            read( s, "alpha:contested", who );
            assertEquals( BOTH, who[0] );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  Across files, the earlier CLASSPATH entry wins -- the JVM's usual rule for a contested
     *  resource, and the OPPOSITE of how resource paths compose, where later wins.
     */
    public void testAcrossFilesTheEarlierClasspathEntryWins()
    {
        CfgScenario s = scenario( root( ALPHA ), root( BOTH ) );
        try
        {
            String[] who = new String[1];
            read( s, "alpha:contested", who );
            assertEquals( "the earlier classpath entry's resolver should decide", ALPHA, who[0] );
        }
        finally
        { s.closeQuietly(); }
    }

    /** The complement, so neither direction can pass by accident. */
    public void testReversingTheRootsReversesWhoAnswers()
    {
        CfgScenario s = scenario( root( BOTH ), root( ALPHA ) );
        try
        {
            String[] who = new String[1];
            read( s, "alpha:contested", who );
            assertEquals( BOTH, who[0] );
        }
        finally
        { s.closeQuietly(); }
    }

    /** A resolver that declines passes the question on rather than ending the search. */
    public void testAResolverThatDeclinesDoesNotEndTheSearch()
    {
        CfgScenario s = scenario( root( ALPHA, BETA ) );
        try
        {
            String[] who = new String[1];
            read( s, "beta:something", who );
            assertEquals( "alpha declines beta:, so beta's resolver should answer", BETA, who[0] );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------------ the file's syntax

    /**
     *  The line grammar, form by form.
     *
     *  <p>Same rules as {@code mlog-config-resource-paths.txt}: a first non-whitespace {@code #}
     *  makes a comment line, an empty or all-whitespace line is ignored, and otherwise the line
     *  is cut at a {@code #} <b>preceded by whitespace</b> and then <b>trimmed again</b>. The
     *  second trim is the one that matters here: without it a trailing comment would leave
     *  whitespace on the end of a class name, and {@code Class.forName} on
     *  {@code "com.example.Foo "} fails with a ClassNotFoundException naming a class that looks
     *  exactly right in the log.</p>
     *
     *  <p>The whitespace requirement is not needed for class names, which cannot contain
     *  {@code #} &mdash; it is needed for resource identifiers, which can. Both files use the one
     *  grammar anyway, so that a deployment author learns one rule rather than two, and
     *  {@link #testATightHashIsPartOfTheNameNotAComment} holds the two in step.</p>
     *
     *  <p>Asserted one form per scenario rather than all at once, so a failure names which form
     *  broke. Each case declares a single line and asks whether the resolver answered -- which is
     *  the only thing observable from outside, since a name that parsed wrongly simply fails to
     *  instantiate.</p>
     */
    public void testTheLineGrammarFormByForm()
    {
        assertDeclares( "a bare name",                        ALPHA );
        assertDeclares( "surrounding whitespace",             "   " + ALPHA + "   " );
        assertDeclares( "a trailing comment",                 ALPHA + " # why this one" );
        assertDeclares( "padding either side of the comment", "  " + ALPHA + "   #   why this one" );
        assertDeclares( "several hashes",                     ALPHA + " ## ### #" );

        assertDeclaresNothing( "a comment in column one",     "#" + ALPHA );
        assertDeclaresNothing( "a comment after whitespace",  "    #" + ALPHA );
        assertDeclaresNothing( "an empty file",               "" );
        assertDeclaresNothing( "only whitespace",             "      " );
        assertDeclaresNothing( "only a tab",                  "\t" );
    }

    private void assertDeclares( String what, String line )
    {
        CfgScenario s = scenario( root( line ) );
        try
        {
            String[] who = new String[1];
            read( s, "alpha:something", who );
            assertEquals( what + " should declare the resolver", ALPHA, who[0] );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  A comment or blank line must be <b>recognized as one</b>, not merely fail to name a
     *  resolver.
     *
     *  <p>The second assertion is what makes this test worth anything. Treat a comment line as an
     *  entry and it parses to the empty string, which names no class, so nothing answers and a
     *  test asking only "did anything answer?" passes. The tell is the diagnostic: an entry that
     *  cannot be instantiated is reported, and a line correctly understood as a comment produces
     *  no such report. Two mutations -- not skipping comment lines, and testing the untrimmed
     *  line for a leading {@code #} -- are invisible without it.</p>
     */
    private void assertDeclaresNothing( String what, String line )
    {
        CfgScenario s = scenario( root( line ) );
        try
        {
            String[] who = new String[1];
            List<String[]> items = read( s, "alpha:something", who );

            assertNull( what + " should declare nothing, but something answered", who[0] );
            assertFalse( what + " should be understood as a comment or blank, not as an entry " +
                         "that failed to instantiate: " + describe( items ),
                         says( items, "WARNING", "Could not instantiate" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  And the trailing-comment trim specifically, from the other side: a name left with
     *  trailing whitespace would be reported as un-instantiable, so no such warning may appear.
     *
     *  <p>Worth asserting separately because the case above would also pass if the name were
     *  mangled in some way that happened to still resolve. This one watches for the diagnostic
     *  that a mangled name would produce.</p>
     */
    public void testATrailingCommentLeavesNoWhitespaceOnTheName()
    {
        CfgScenario s = scenario( root( ALPHA + "    # padded comment" ) );
        try
        {
            String[] who = new String[1];
            List<String[]> items = read( s, "alpha:something", who );

            assertEquals( ALPHA, who[0] );
            assertFalse( "a name with trailing whitespace would be reported un-instantiable: " + describe( items ),
                         says( items, "WARNING", "Could not instantiate" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  A {@code #} with no whitespace before it is part of the entry, not the start of a comment
     *  -- here too, even though no class name could contain one.
     *
     *  <p>The rule exists for the other file, where identifiers legitimately carry a {@code #}:
     *  a HOCON scope, a URL fragment. Applying it uniformly costs nothing and means there is one
     *  line grammar to learn rather than two that differ in a corner. What it looks like here is
     *  that a malformed name is reported as a malformed name, rather than being quietly truncated
     *  into a different one that might even exist.</p>
     */
    public void testATightHashIsPartOfTheNameNotAComment()
    {
        CfgScenario s = scenario( root( ALPHA + "#not-a-comment" ) );
        try
        {
            String[] who = new String[1];
            List<String[]> items = read( s, "alpha:something", who );

            assertNull( "the line names no loadable class, so nothing should answer", who[0] );
            assertTrue( "and the whole line should be reported as the name it tried: " + describe( items ),
                        says( items, "WARNING", ALPHA + "#not-a-comment" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  A class named in the file that cannot be instantiated is reported at WARNING, and the
     *  search carries on to the next resolver.
     *
     *  <p>The level matters and has been wrong: with the resolver list cached, the items were
     *  produced once and handed to whichever caller resolved first -- which was the internal
     *  "is this vetoable?" probe, whose items are demoted to FINEST. A real typo in the file was
     *  then invisible at every ordinary log level.</p>
     */
    public void testABadClassNameIsReportedAtWarningAndTheSearchContinues()
    {
        CfgScenario s = scenario( root( "com.example.NoSuchResolverAnywhere", ALPHA ) );
        try
        {
            String[] who = new String[1];
            List<String[]> items = read( s, "alpha:something", who );

            assertTrue( "the unusable class name should be named at WARNING: " + describe( items ),
                        says( items, "WARNING", "NoSuchResolverAnywhere" ) );
            assertEquals( "and the next resolver should still have answered", ALPHA, who[0] );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------------ vetoability

    /**
     *  An extension can make an identifier refusable, and plain {@code AsProvided} must then
     *  decline to read it rather than risk the refusal.
     *
     *  <p>Vetoability is not a property of the identifier but of the source a resolver returns,
     *  so the library can only learn it by resolving. That makes this the one guarantee an
     *  extension point could accidentally route around: if extension-supplied sources were not
     *  checked, {@code AsProvided} would silently gain the ability to throw
     *  {@code ConfigVetoedException}, which its whole contract is that it cannot.</p>
     */
    public void testAnExtensionSuppliedVetoableSourceIsRefusedByAsProvided()
    {
        CfgScenario s = scenario( root( VETO ) );
        try
        {
            try
            {
                s.asProvidedUncached( new String[] { "vetoable:something" }, new ArrayList() );
                fail( "AsProvided must refuse an identifier whose source can veto" );
            }
            catch ( RuntimeException expected )
            {
                String msg = String.valueOf( expected.getMessage() ) + " / " +
                             String.valueOf( expected.getCause() == null ? "" : expected.getCause().getMessage() );
                assertTrue( "the refusal should name the vetoable identifier, got: " + msg,
                            msg.contains( "vetoable:something" ) );
            }
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------------ the cache

    /**
     *  The resolver list is resolved once per ClassLoader and then fixed: a resource appearing
     *  afterwards is NOT picked up.
     *
     *  <p>A deliberate choice rather than an accident of implementation. The list is a dispatch
     *  table, not configuration -- it decides how an identifier is <i>interpreted</i> -- and a
     *  table that could change mid-JVM would let one identifier string mean different things at
     *  different moments, while {@code MConfig}'s own cache keys on that string as a stable name.
     *  It would also make resolution unstable <i>within</i> a single read, which performs the
     *  vetoable check and the read itself as separate resolutions.</p>
     *
     *  <p>An earlier version cached only when the resource was found, using null for both "not
     *  yet looked" and "looked, found nothing" -- so in the ordinary case of no extras at all
     *  the classpath was rescanned forever and this invariant quietly did not hold.</p>
     */
    public void testTheResolverListIsFixedOnceResolved()
    {
        File late = root();   // an extras root that starts out EMPTY
        new File( late, RESOURCE ).delete();

        CfgScenario s = scenario( late );
        try
        {
            String[] who = new String[1];
            read( s, "alpha:something", who );
            assertNull( "precondition: nothing is declared yet", who[0] );

            write( new File( late, RESOURCE ), ALPHA );

            read( s, "alpha:something", who );
            assertNull( "a resolver declared after the first resolution must not be picked up", who[0] );
        }
        catch ( Exception e )
        { throw new RuntimeException( e ); }
        finally
        { s.closeQuietly(); }
    }

    /** And the complement: declared before the first resolution, it is used. */
    public void testAResolverDeclaredBeforeTheFirstResolutionIsUsed()
    {
        CfgScenario s = scenario( root( ALPHA ) );
        try
        {
            String[] who = new String[1];
            read( s, "alpha:something", who );
            assertEquals( ALPHA, who[0] );
        }
        finally
        { s.closeQuietly(); }
    }
}
