package com.mchange.v2.cfg.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 *  A properties resource path may be satisfied by more than one resource -- one per classpath
 *  entry that carries that path -- and BasicPropertiesConfigSource now merges them all rather
 *  than reading only the first. That lets a framework jar ship defaults at
 *  /mchange-commons.properties and an application jar override some of them, without the
 *  application having to restate the whole file.
 *
 *  <p>Precedence follows the classpath: earlier entries win. That is the same resource
 *  Class.getResourceAsStream would have returned before this change, so for any deployment with
 *  one resource per path the behaviour is exactly what it was, and for deployments with several
 *  the winner is the one they would already have got. The change adds the losers' keys; it does
 *  not reshuffle the winner.</p>
 *
 *  <p>Testing this needs several classpath entries carrying one path, and the source resolves
 *  against its <i>own</i> ClassLoader rather than the thread context one -- so there is no
 *  injecting a loader from outside. CfgScenario is the existing answer: it builds a
 *  URLClassLoader parented to the platform loader, with the library's own code source on it, so
 *  the library is loaded fresh inside that loader and sees only the scenario's classpath. The
 *  source is then driven reflectively, since the copy under test is not the copy this test was
 *  compiled against.</p>
 */
public class MergedPropertiesResourcesJUnitTestCase extends TestCase
{
    private final static String PATH = "/merge-probe.properties";

    private List<File> roots;

    @Override
    protected void setUp() throws Exception
    { roots = new ArrayList<File>(); }

    @Override
    protected void tearDown() throws Exception
    {
        for ( File root : roots )
        {
            File f = new File( root, "merge-probe.properties" );
            if ( f.exists() ) f.delete();
            root.delete();
        }
    }

    /**
     *  A classpath root carrying merge-probe.properties with the given lines. Returned in the
     *  order created, which is the order they are put on the classpath -- so the first one made
     *  is the highest priority.
     */
    private File root( String... lines ) throws Exception
    {
        File dir = File.createTempFile( "merge-root", "" );
        dir.delete();
        dir.mkdirs();
        PrintWriter pw = new PrintWriter( new File( dir, "merge-probe.properties" ), "ISO-8859-1" );
        try { for ( String line : lines ) pw.println( line ); }
        finally { pw.close(); }
        roots.add( dir );
        return dir;
    }

    /** What the source produced, rendered so it can be asserted on across the loader boundary. */
    private static class Result
    {
        Properties     properties;
        List<String[]> items;       // [level, text, exceptionClassName]
        Throwable      thrown;
    }

    private Result read( String identifier, File... extraRoots ) throws Exception
    {
        CfgScenario s = CfgScenario.openWithRoots( "no-pathfiles", false, extraRoots );
        try
        {
            Class<?> srcClass = s.classLoader().loadClass( "com.mchange.v2.cfg.BasicPropertiesConfigSource" );
            Object   src      = srcClass.getConstructor().newInstance();
            Method   m        = srcClass.getMethod( "propertiesFromSource", String.class );

            Result out = new Result();
            try
            {
                Object parse = m.invoke( src, identifier );
                out.properties = (Properties) parse.getClass().getMethod( "getProperties" ).invoke( parse );
                out.items = s.renderLogItems( (List) parse.getClass().getMethod( "getDelayedLogItems" ).invoke( parse ) );
            }
            catch ( InvocationTargetException ite )
            {
                out.thrown = ite.getCause();
                out.items  = new ArrayList<String[]>();
            }
            return out;
        }
        finally { s.closeQuietly(); }
    }

    // ==================== the merge ====================

    /** The point of the exercise: keys from every resource at the path, not just the first. */
    public void testKeysFromEveryResourceAreMerged() throws Exception
    {
        File first  = root( "fromFirst=1",  "shared=first-wins" );
        File second = root( "fromSecond=2", "alsoSecond=3" );

        Result r = read( PATH, first, second );

        assertNull( "" + r.thrown, r.thrown );
        assertEquals( "a key only the higher-priority resource has", "1", r.properties.getProperty( "fromFirst" ) );
        assertEquals( "a key only the lower-priority resource has -- this is what the merge adds",
                      "2", r.properties.getProperty( "fromSecond" ) );
        assertEquals( "3", r.properties.getProperty( "alsoSecond" ) );
    }

    /**
     *  Precedence is classpath order, earliest wins. This is the assertion that keeps the change
     *  compatible: the winner is the resource Class.getResourceAsStream would have returned.
     */
    public void testTheEarliestClasspathEntryWins() throws Exception
    {
        File first  = root( "shared=from-first" );
        File second = root( "shared=from-second" );

        assertEquals( "from-first", read( PATH, first, second ).properties.getProperty( "shared" ) );
        assertEquals( "and the ordering really is what decides, not the content",
                      "from-second", read( PATH, second, first ).properties.getProperty( "shared" ) );
    }

    /** Three entries layer the same way, rather than only the outermost pair interacting. */
    public void testThreeResourcesLayerInClasspathOrder() throws Exception
    {
        File a = root( "only_a=a", "ab=from-a", "abc=from-a" );
        File b = root( "only_b=b", "ab=from-b", "abc=from-b" );
        File c = root( "only_c=c",             "abc=from-c" );

        Result r = read( PATH, a, b, c );

        assertEquals( "a", r.properties.getProperty( "only_a" ) );
        assertEquals( "b", r.properties.getProperty( "only_b" ) );
        assertEquals( "c", r.properties.getProperty( "only_c" ) );
        assertEquals( "from-a", r.properties.getProperty( "ab" ) );
        assertEquals( "from-a", r.properties.getProperty( "abc" ) );
        assertEquals( "five distinct keys across three resources: " + r.properties, 5, r.properties.size() );
    }

    /** One resource is the ordinary case, and must behave exactly as it always did. */
    public void testASingleResourceIsUnaffected() throws Exception
    {
        File only = root( "solo=value" );

        Result r = read( PATH, only );

        assertEquals( "value", r.properties.getProperty( "solo" ) );
        assertEquals( 1, r.properties.size() );
        assertFalse( "and nothing is overridden, so nothing is reported: " + CfgScenario.describe( r.items ),
                     CfgScenario.hasLogItem( r.items, "WARNING", "overrides" ) );
    }

    // ==================== what gets reported ====================

    /**
     *  A key defined in two resources is a precedence decision made silently unless reported,
     *  and the deployment cannot see the classpath from inside its configuration. Rare enough to
     *  warrant WARNING: the usual deployment has one resource per path.
     */
    public void testAnOverriddenKeyIsReported() throws Exception
    {
        File first  = root( "shared=from-first", "onlyFirst=x" );
        File second = root( "shared=from-second" );

        Result r = read( PATH, first, second );

        assertTrue( "the override should be reported: " + CfgScenario.describe( r.items ),
                    CfgScenario.hasLogItem( r.items, "WARNING", "overrides" ) );
        assertTrue( "naming the key that was overridden: " + CfgScenario.describe( r.items ),
                    CfgScenario.hasLogItem( r.items, "WARNING", "shared" ) );
    }

    /**
     *  And naming only the keys actually overridden. A resource with fifty keys that overrides
     *  one must not claim to override fifty -- the report is what a deployer uses to find the
     *  collision, so it has to be the collision and not the file's whole contents.
     */
    public void testTheReportNamesOnlyTheOverlappingKeys() throws Exception
    {
        File first  = root( "shared=from-first", "privateToFirst=unique" );
        File second = root( "shared=from-second" );

        Result r = read( PATH, first, second );

        String text = textOfFirstWarningMentioning( r.items, "overrides" );
        assertNotNull( "precondition: an override was reported", text );
        assertTrue( "the overlapping key belongs in the report: " + text, text.indexOf( "shared" ) >= 0 );
        assertFalse( "a key present in only one resource overrides nothing: " + text,
                     text.indexOf( "privateToFirst" ) >= 0 );
    }

    private static String textOfFirstWarningMentioning( List<String[]> items, String fragment )
    {
        for ( String[] item : items )
            if ( "WARNING".equals( item[0] ) && item[1] != null && item[1].contains( fragment ) )
                return item[1];
        return null;
    }

    // ==================== resilience across several resources ====================

    /**
     *  An unreadable resource among several must not deny the deployment the readable ones --
     *  which is the whole reason a bad file is reported rather than thrown. It must also
     *  contribute nothing of its own: Properties.load populates as it goes, so the keys before
     *  the malformed escape are sitting in a half-built Properties when it throws, and merging
     *  those would be worse than skipping the file.
     */
    public void testOneUnreadableResourceDoesNotDenyTheOthers() throws Exception
    {
        File good = root( "fromGood=yes" );
        File bad  = root( "beforeBad=never-merged", "bad=x\\uZZZZ", "afterBad=never-reached" );

        Result r = read( PATH, good, bad );

        assertNull( "the readable resource should still be delivered: " + r.thrown, r.thrown );
        assertEquals( "yes", r.properties.getProperty( "fromGood" ) );
        assertNull( "the unreadable resource contributes nothing, not even its parseable prefix: "
                    + r.properties,
                    r.properties.getProperty( "beforeBad" ) );
        assertTrue( "and it is reported: " + CfgScenario.describe( r.items ),
                    CfgScenario.hasLogItem( r.items, "WARNING", "An Exception occurred while trying to load" ) );
    }

    /** The same holds when the unreadable resource is the higher-priority one. */
    public void testAnUnreadableHigherPriorityResourceStillYieldsToTheReadableOne() throws Exception
    {
        File bad  = root( "beforeBad=never-merged", "bad=x\\uZZZZ" );
        File good = root( "fromGood=yes", "beforeBad=from-good" );

        Result r = read( PATH, bad, good );

        assertEquals( "yes", r.properties.getProperty( "fromGood" ) );
        assertEquals( "the lower-priority value stands, the broken file having contributed nothing",
                      "from-good", r.properties.getProperty( "beforeBad" ) );
    }

    // ==================== absence is still absence ====================

    /**
     *  No resources at all is still an absence, and must still throw -- that is what
     *  BasicMultiPropertiesConfig reads as "drop this path and keep the others" and files under
     *  not-found. Easy to lose when the accumulator is created up front rather than on demand,
     *  because then it is never null and the absence branch becomes unreachable.
     */
    public void testNoResourcesAtAllIsStillAnAbsence() throws Exception
    {
        File unrelated = root( "irrelevant=1" );

        Result r = read( "/no-such-merge-path.properties", unrelated );

        assertNotNull( "an absence must be signalled, not returned as an empty success", r.thrown );
        assertTrue( "and as a FileNotFoundException, which is the type the framework reads as "
                    + "absent: " + r.thrown,
                    r.thrown instanceof FileNotFoundException );
    }
}
