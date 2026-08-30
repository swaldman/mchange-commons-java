package com.mchange.v2.cfg;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 *  Lives in com.mchange.v2.cfg rather than com.mchange.v2.cfg.junit because it drives the
 *  package-private BasicMultiPropertiesConfig constructors directly. That is the point: the
 *  behavior under test is how those constructors plumb DelayedLogItems, and the public facade
 *  always hands them a list, so it cannot reach the cases that have actually broken.
 *
 *  <p>A BasicMultiPropertiesConfig builds in two phases. firstInit() reads the sources;
 *  finishInit() indexes what they yielded into the by-prefix and by-key maps. <b>Both phases
 *  emit DelayedLogItems.</b> Two decisions follow from that, and both have been got wrong:</p>
 *
 *  <ul>
 *    <li><p><b>Which list the two phases write into.</b> The caller may pass none, and then one
 *    must be fabricated -- once, for both phases, or items from whichever phase got the other
 *    list are lost. Fabricating per-phase has silently dropped finishInit's items; handing
 *    finishInit a raw null has thrown NullPointerException out of a constructor, but only when
 *    a diagnostic actually fired, so it hid.</p></li>
 *
 *    <li><p><b>When the config's own copy of that list is taken.</b> It must be a copy, or the
 *    caller's list is aliased and a long-lived config keeps pinning whatever the caller adds
 *    later -- but a copy taken at the end of firstInit is taken before finishInit has run, and
 *    loses everything finishInit says.</p></li>
 *  </ul>
 *
 *  <p>The one source that can make finishInit report is system properties: everything else
 *  arrives through Properties.load and is String-keyed by construction. So these tests put a
 *  non-String key into System.getProperties() and read "/". The build runs test classes
 *  serially in one JVM, so that is safe as long as the key is removed again.</p>
 */
public final class DelayedLogItemPlumbingInternalJUnitTestCase extends TestCase
{
    private final static String NOT_A_STRING_KEY = "contains a key that is not a String";
    private final static String VETOED           = "has vetoed config";

    private final static String MISSING_RESOURCE = "/definitely-absent-from-the-classpath.properties";

    private Object nonStringKey = null;

    protected void tearDown() throws Exception
    {
        if ( nonStringKey != null )
        {
            System.getProperties().remove( nonStringKey );
            nonStringKey = null;
        }
    }

    /** Makes finishInit() -- and only finishInit() -- emit DelayedLogItems on a read of "/". */
    private void makeFinishInitReport()
    {
        nonStringKey = new Object();
        System.getProperties().put( nonStringKey, "a value under a key that is not a String" );
    }

    /**
     *  finishInit()'s items must reach the config, not just the caller's list.
     *
     *  <p>The copy into parseMessages was once taken in a finally at the end of firstInit, which
     *  runs before finishInit. The caller's list held the items and the config's own list did
     *  not, so getDelayedLogItems() reported nothing at all about them.</p>
     */
    public void testFinishInitItemsReachGetDelayedLogItems()
    {
        makeFinishInitReport();

        List callersList = new ArrayList();
        BasicMultiPropertiesConfig c =
            new BasicMultiPropertiesConfig( MConfig.Kind.AsProvided, new String[] { "/" }, callersList );

        assertTrue( "the caller's list should have received finishInit's report; got: " + describe( callersList ),
                    hasItemContaining( callersList, NOT_A_STRING_KEY ) );
        assertTrue( "and so should the config itself; got: " + describe( c.getDelayedLogItems() ),
                    hasItemContaining( c.getDelayedLogItems(), NOT_A_STRING_KEY ) );
    }

    /**
     *  A null delayedLogItems must not become a NullPointerException when finishInit reports.
     *
     *  <p>Once the constructors took over fabricating the list, one of them kept passing the raw
     *  parameter to finishInit rather than the fabricated one. Nothing failed until a diagnostic
     *  actually fired -- and then it threw straight out of the public
     *  BasicMultiPropertiesConfig(String[]) constructor.</p>
     */
    public void testNullDelayedLogItemsSurvivesFinishInitReporting()
    {
        makeFinishInitReport();

        BasicMultiPropertiesConfig c =
            new BasicMultiPropertiesConfig( MConfig.Kind.AsProvided, new String[] { "/" }, null );

        assertTrue( "finishInit's report must be retained even with no caller list; got: "
                        + describe( c.getDelayedLogItems() ),
                    hasItemContaining( c.getDelayedLogItems(), NOT_A_STRING_KEY ) );
    }

    /**
     *  With no caller list, items from BOTH phases must survive.
     *
     *  <p>This is the assertion that pins down the fix. Moving the copy from the end of firstInit
     *  to the end of finishInit repairs {@link #testFinishInitItemsReachGetDelayedLogItems} while
     *  quietly breaking this one, because each phase would then fabricate a list of its own and
     *  the copy would see only the second. One list for both phases, copied once at the end, is
     *  the only arrangement that satisfies both.</p>
     */
    public void testItemsFromBothInitPhasesAreRetained()
    {
        makeFinishInitReport();

        BasicMultiPropertiesConfig c =
            new BasicMultiPropertiesConfig( MConfig.Kind.AsProvided, new String[] { MISSING_RESOURCE, "/" }, null );

        List items = c.getDelayedLogItems();

        assertTrue( "firstInit's skip notice is missing; got: " + describe( items ),
                    hasItemContaining( items, MISSING_RESOURCE ) );
        assertTrue( "finishInit's report is missing; got: " + describe( items ),
                    hasItemContaining( items, NOT_A_STRING_KEY ) );
    }

    /**
     *  parseMessages must be a copy, never a view of the caller's list.
     *
     *  <p>It was once wrapped with Collections.unmodifiableList directly, which is unmodifiable
     *  only from the config's side: the caller still held the backing list. ConfigUtils'
     *  canonicalDefaultConfig is a static that lives as long as its ClassLoader, so aliasing
     *  there meant pinning every Throwable the caller went on to add.</p>
     */
    public void testParseMessagesDoesNotAliasTheCallersList()
    {
        List callersList = new ArrayList();
        BasicMultiPropertiesConfig c =
            new BasicMultiPropertiesConfig( MConfig.Kind.AsProvided, new String[] { MISSING_RESOURCE }, callersList );

        int before = c.getDelayedLogItems().size();
        callersList.add( new DelayedLogItem( DelayedLogItem.Level.WARNING,
                                             "added by the caller after construction",
                                             new RuntimeException( "and this would be pinned with it" ) ) );

        assertEquals( "the config must not see what the caller adds afterwards",
                      before, c.getDelayedLogItems().size() );
    }

    /**
     *  A veto reported to no one is a veto reported to stderr.
     *
     *  <p>ConfigVetoedException carries its own delayed items, but the vetoable constructor throws
     *  before its caller can read anything off the config -- there is no config. When the caller
     *  supplied no list, the stderr dump was the only report, and dropping firstInit's try/finally
     *  removed it: the read failed with the WARNING that explains why vanishing entirely.</p>
     */
    public void testVetoWithNullDelayedLogItemsIsStillReportedToSysErr() throws Exception
    {
        String identifier = missingRequiredFileUrl();

        PrintStream            oldErr = System.err;
        ByteArrayOutputStream  caught = new ByteArrayOutputStream();
        try
        {
            System.setErr( new PrintStream( caught, true, "UTF-8" ) );
            try
            {
                new BasicMultiPropertiesConfig( BasicMultiPropertiesConfig.VetoThrowing.INSTANCE,
                                                new String[] { identifier },
                                                null );
                fail( "expected ConfigVetoedException for a required file that does not exist" );
            }
            catch ( ConfigVetoedException expected )
            { /* the veto itself is covered elsewhere; what matters here is what got reported */ }
        }
        finally
        { System.setErr( oldErr ); }

        String dumped = caught.toString( "UTF-8" );
        assertTrue( "the veto must still reach stderr when there is no list to collect it; got: " + dumped,
                    dumped.contains( VETOED ) );
    }

    /**
     *  The other half: given a list, the veto's items go into it, and stderr stays quiet. Guards
     *  against restoring the stderr dump by making it unconditional.
     */
    public void testVetoWithACallerListReportsToTheListAndNotSysErr() throws Exception
    {
        String identifier = missingRequiredFileUrl();
        List   callersList = new ArrayList();

        PrintStream            oldErr = System.err;
        ByteArrayOutputStream  caught = new ByteArrayOutputStream();
        try
        {
            System.setErr( new PrintStream( caught, true, "UTF-8" ) );
            try
            {
                new BasicMultiPropertiesConfig( BasicMultiPropertiesConfig.VetoThrowing.INSTANCE,
                                                new String[] { identifier },
                                                callersList );
                fail( "expected ConfigVetoedException for a required file that does not exist" );
            }
            catch ( ConfigVetoedException expected )
            { /* as above */ }
        }
        finally
        { System.setErr( oldErr ); }

        String dumped = caught.toString( "UTF-8" );
        assertTrue( "the caller's list should have collected the veto; got: " + describe( callersList ),
                    hasItemContaining( callersList, VETOED ) );
        assertFalse( "the veto went to the list, so it should not also go to stderr; got: " + dumped,
                     dumped.contains( VETOED ) );
    }

    // ------------------------------------------------------------- fixture

    /**
     *  A file: URL naming a file that does not exist, marked required -- which the file: source
     *  vetoes. No fixture is created: absence is the whole point, and it is asserted rather than
     *  assumed.
     */
    private String missingRequiredFileUrl()
    {
        Path p = Paths.get( System.getProperty( "java.io.tmpdir" ) )
                      .resolve( "mchange-cfg-absent-" + System.nanoTime() + ".properties" );
        assertFalse( "fixture assumes this file does not exist: " + p, Files.exists( p ) );
        return p.toUri().toString() + "?required=true";
    }

    // ------------------------------------------------------------- helpers

    private static boolean hasItemContaining( List items, String substring )
    {
        for ( Object o : items )
            if ( ((DelayedLogItem) o).getText().contains( substring ) )
                return true;
        return false;
    }

    private static String describe( List items )
    {
        StringBuffer sb = new StringBuffer();
        for ( Object o : items )
        {
            DelayedLogItem item = (DelayedLogItem) o;
            sb.append( "\n    [" ).append( item.getLevel() ).append( "] " ).append( item.getText() );
        }
        return sb.length() == 0 ? "(no items)" : sb.toString();
    }
}
