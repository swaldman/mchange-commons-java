package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 *  The MConfig javadoc calls this out as counterintuitive, so it is worth pinning:
 *
 *    "those hardcoded backstop config locations ... are in effect whenever the
 *     built-in default resource-path locations supply no resource paths, even if
 *     the developer explicitly provides their own 'preemptingResources' and
 *     'defaultResources' config locations."
 *
 *  The backstop is spliced BETWEEN the caller's defaults and the caller's preempts,
 *  so caller defaults lose to it and caller preempts beat it.
 *
 *  The AsProvided facade is the escape hatch: it reads only what the caller names.
 */
public final class BackstopDefaultsJUnitTestCase extends TestCase
{
    private final static String[] CALLER_DEFAULT = new String[] { "/caller-default.properties" };
    private final static String[] CALLER_PREEMPT = new String[] { "/caller-preempt.properties" };

    private static List<String> readPaths( CfgScenario s, Object mpc )
    { return Arrays.asList( s.getPropertiesResourcePaths( mpc ) ); }

    /**
     *  With no path files present, caller-supplied resources do NOT displace the
     *  hardcoded backstop; the backstop lands between them.
     */
    public void testBackstopAppliesEvenWithCallerSuppliedResources()
    {
        CfgScenario s = CfgScenario.open( "no-pathfiles" );
        try
        {
            Object mpc = s.traditionalCached( CALLER_DEFAULT, CALLER_PREEMPT );

            assertEquals( "the backstop's /mchange-commons.properties should still be read",
                          "mchange-commons", s.getProperty( mpc, "backstop.source" ) );

            List<String> paths = readPaths( s, mpc );
            assertEquals( "resolved order should be: caller defaults, backstop, caller preempts",
                          Arrays.asList( "/caller-default.properties",
                                         "/mchange-commons.properties",
                                         "hocon:/reference,/application,/",
                                         "/",
                                         "/caller-preempt.properties" ),
                          paths );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Caller preempts outrank the backstop; caller defaults are outranked by it. */
    public void testBackstopSitsBetweenCallerDefaultsAndPreempts()
    {
        CfgScenario s = CfgScenario.open( "no-pathfiles" );
        try
        {
            Object withBoth = s.traditionalCached( CALLER_DEFAULT, CALLER_PREEMPT );
            assertEquals( "caller preempts should beat the backstop",
                          "from-caller-preempt", s.getProperty( withBoth, "layered.key" ) );

            Object defaultsOnly = s.traditionalCached( CALLER_DEFAULT, new String[0] );
            assertEquals( "the backstop should beat caller defaults",
                          "from-mchange-commons", s.getProperty( defaultsOnly, "layered.key" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** AsProvided reads only what it is given -- no backstop, no path files. */
    public void testAsProvidedDoesNotApplyBackstop()
    {
        CfgScenario s = CfgScenario.open( "no-pathfiles" );
        try
        {
            Object mpc = s.asProvidedCached( CALLER_DEFAULT, CALLER_PREEMPT, null );

            assertNull( "AsProvided must not consult /mchange-commons.properties",
                        s.getProperty( mpc, "backstop.source" ) );
            assertEquals( Arrays.asList( "/caller-default.properties", "/caller-preempt.properties" ),
                          readPaths( s, mpc ) );
            assertEquals( "from-caller-preempt", s.getProperty( mpc, "layered.key" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** The single-list AsProvided form treats its argument as preempting resources. */
    public void testAsProvidedSingleListForm()
    {
        CfgScenario s = CfgScenario.open( "no-pathfiles" );
        try
        {
            Object mpc = s.asProvidedCached(
                new String[] { "/caller-default.properties", "/caller-preempt.properties" } );
            assertEquals( Arrays.asList( "/caller-default.properties", "/caller-preempt.properties" ),
                          readPaths( s, mpc ) );
            assertEquals( "from-caller-preempt", s.getProperty( mpc, "layered.key" ) );
            assertNull( s.getProperty( mpc, "backstop.source" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  The documented escape hatch for the traditional API: supply a path file, and the
     *  backstop is suppressed even though caller resources are also in play.
     */
    public void testPathFileSuppressesBackstopForTraditionalApi()
    {
        CfgScenario s = CfgScenario.open( "vmconfig-only" );
        try
        {
            Object mpc = s.traditionalCached( new String[0], CALLER_PREEMPT );

            assertNull( "backstop should be suppressed by the path file",
                        s.getProperty( mpc, "backstop.source" ) );
            assertEquals( "vmconfig", s.getProperty( mpc, "which.pathfile" ) );
        }
        finally
        { s.closeQuietly(); }
    }
}
