package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import com.mchange.v2.cfg.ConfigParseException;
import com.mchange.v2.cfg.ConfigVetoedException;
import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.MConfig;
import com.mchange.v2.cfg.MultiPropertiesConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 *  The reporting contract that spans every PropertiesConfigSource: if a source accumulates
 *  DelayedLogItems and then throws, those items must still reach the caller.
 *
 *  <p>They cannot get there the ordinary way. A source reports through the Parse it returns,
 *  and a Parse is built only on the success path -- so anything accumulated before a throw is
 *  discarded with the stack frame unless the exception carries it out. That is what
 *  ConfigParseException is for, and this test case is the guard on it. The failure it prevents
 *  is invisible by nature: nothing breaks, no test goes red, the read simply goes quiet about
 *  something it had already decided was worth saying.</p>
 *
 *  <p>Covered here for both shapes the contract takes -- a source that vetoes, and a source
 *  that merely finds nothing -- because they leave through different exception types and are
 *  drained by different catch clauses.</p>
 */
public final class ConfigParseReportingJUnitTestCase extends TestCase
{
    /** No caller-supplied resources on the defaults side of the read. */
    private final static String[] NONE = new String[0];

    private Path dir;

    protected void setUp() throws Exception
    { dir = Files.createTempDirectory( "mchange-cfg-reporting-" ).toRealPath(); }

    protected void tearDown() throws Exception
    { try { Files.deleteIfExists( dir ); } catch ( IOException e ) { /* best effort */ } }

    private boolean posixSupported()
    { return dir.getFileSystem().supportedFileAttributeViews().contains( "posix" ); }

    /** An identifier that vetoes: useronly makes the source check, required=true makes absence fatal. */
    private String vetoingAbsentUrl()
    { return dir.resolve( "absent.properties" ).toUri().toString() + "?permissions=useronly&required=true"; }

    private static boolean hasItem( List<DelayedLogItem> items, String fragment )
    {
        for ( DelayedLogItem item : items )
            if ( item.getText() != null && item.getText().contains( fragment ) ) return true;
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<DelayedLogItem> itemsOf( MultiPropertiesConfig mpc )
    { return mpc.getDelayedLogItems(); }

    // ------------------------------------------------------- a source that vetoes

    /**
     *  The vetoing source had something to say before it threw, and the caller must hear it.
     *
     *  The read throws, so no MultiPropertiesConfig is ever returned and there is no Parse to
     *  read items from. The only surviving channel is the list the caller passed in, which
     *  BasicMultiPropertiesConfig publishes in a finally block after draining the exception.
     */
    public void testVetoingSourceReportsReachTheCallersList()
    {
        if ( !posixSupported() ) return;

        List<DelayedLogItem> captured = new ArrayList<DelayedLogItem>();
        try
        {
            MConfig.AsProvidedVetoable.readUncachedClassloaderResourceConfig(
                NONE, new String[] { vetoingAbsentUrl() }, captured );
            fail( "expected a veto" );
        }
        catch ( ConfigVetoedException expected )
        {
            assertTrue( "the item the source prepared before throwing was lost: " + captured,
                        hasItem( captured, "was not present when its permissions were checked" ) );
        }
    }

    /** And the exception carries them itself, which is how they got there. */
    public void testVetoExceptionCarriesTheItems()
    {
        if ( !posixSupported() ) return;

        try
        {
            MConfig.AsProvidedVetoable.readUncachedClassloaderResourceConfig(
                NONE, new String[] { vetoingAbsentUrl() }, new ArrayList() );
            fail( "expected a veto" );
        }
        catch ( ConfigVetoedException expected )
        {
            assertTrue( "a ConfigVetoedException is a ConfigParseException and must carry its report",
                        expected instanceof ConfigParseException );
            assertTrue( "the veto carried no report: " + expected.getDelayedLogItems(),
                        hasItem( expected.getDelayedLogItems(),
                                 "was not present when its permissions were checked" ) );
        }
    }

    /** Never null, so a caller can drain unconditionally. */
    public void testCarriedItemListIsNeverNull()
    {
        if ( !posixSupported() ) return;

        try
        {
            MConfig.AsProvidedVetoable.readUncachedClassloaderResourceConfig(
                NONE, new String[] { vetoingAbsentUrl() }, new ArrayList() );
            fail( "expected a veto" );
        }
        catch ( ConfigVetoedException expected )
        { assertNotNull( expected.getDelayedLogItems() ); }
    }

    // ------------------------------------------- a source that merely finds nothing

    /**
     *  HOCON is the other source that accumulates before throwing, and the one where the loss
     *  actually bit: extractConfig records a diagnostic per resource it fails to find, then
     *  throws when none of them produced a Config. Those per-resource notes are the only thing
     *  that explains WHY nothing was found, and they were discarded at the throw.
     */
    public void testHoconPerResourceDiagnosticsSurviveItsThrow()
    {
        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedClassloaderResourceConfig(
            NONE, new String[] { "hocon:no-such-hocon-resource" }, new ArrayList() );

        assertTrue( "the per-resource diagnostic accumulated before the throw was lost: " + itemsOf( mpc ),
                    hasItem( itemsOf( mpc ), "Missing or empty HOCON configuration for resource path" ) );
    }

    /** The throw site's own summary comes through as well, alongside the accumulated detail. */
    public void testHoconReportsBothTheDetailAndTheSummary()
    {
        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedClassloaderResourceConfig(
            NONE, new String[] { "hocon:no-such-hocon-resource" }, new ArrayList() );

        List<DelayedLogItem> items = itemsOf( mpc );
        assertTrue( "detail missing: "  + items, hasItem( items, "Missing or empty HOCON configuration" ) );
        assertTrue( "summary missing: " + items, hasItem( items, "Could not find HOCON configuration at any" ) );
    }

    /** A source that throws must still drop only its own path, not disturb the read. */
    public void testAThrowingSourceDoesNotDisturbTheSourcesAroundIt()
    {
        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedClassloaderResourceConfig(
            NONE, new String[] { "hocon:no-such-hocon-resource", "/com/mchange/v2/cfg/junit/a.properties" },
            new ArrayList() );

        assertEquals( "the reachable source must still load", "/a/home", mpc.getProperty( "user.home" ) );
    }

    // ------------------------------------------------------------- the ordinary path

    /**
     *  Sources that have nothing of their own to say are still reported FOR, by the framework,
     *  in the familiar words. The carrying contract adds a channel; it does not replace this one.
     */
    public void testOrdinaryAbsenceIsStillReportedByTheFramework()
    {
        MultiPropertiesConfig mpc = MConfig.AsProvided.readUncachedClassloaderResourceConfig(
            NONE, new String[] { "/no-such-resource-anywhere.properties" }, new ArrayList() );

        assertTrue( "the generic skip report should be unaffected: " + itemsOf( mpc ),
                    hasItem( itemsOf( mpc ), "could not be found. Skipping." ) );
    }
}
