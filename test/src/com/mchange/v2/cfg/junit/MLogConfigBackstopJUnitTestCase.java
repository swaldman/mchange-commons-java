package com.mchange.v2.cfg.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 *  MLog's refresh accepts <i>backstops</i> as well as <i>overrides</i>: a layer that supplies a
 *  value where nothing else does, but yields to anything that already did.
 *
 *  <h3>Why MLog has them at all</h3>
 *
 *  <p>They exist so that c3p0 can keep MLog's configuration in step with its own. c3p0 accepts
 *  backstops and overrides, and when <code>com.mchange.v2.c3p0.cfg.syncRefreshLogConfig</code> is
 *  set it forwards the same layers here. Without backstops on this side that forwarding could not
 *  be faithful: a layer c3p0 treated as a floor would arrive as a ceiling, and a deployment's
 *  log settings would be silently overridden by configuration it had marked as a fallback.</p>
 *
 *  <h3>What makes a backstop a backstop</h3>
 *
 *  <p>Position, not a tiebreak. <code>MLogConfig.refresh(..)</code> combines
 *  <code>[backstops..., resources, overrides...]</code>, so "loses to the configuration" and
 *  "beats nothing else" both follow from where the layer sits. These cases pin both ends, because
 *  a layer that lost to everything and a layer that won against everything would each satisfy
 *  half of them.</p>
 *
 *  <p>Each case runs in a CfgScenario: MLogConfig's state is static and one-shot, so a fresh
 *  ClassLoader per case is the only way to get more than one first load per JVM.</p>
 */
public final class MLogConfigBackstopJUnitTestCase extends TestCase
{
    /** No resource-path text files, so the dispatch takes the hardcoded-source-list branch. */
    private final static String SCENARIO = "no-pathfiles";

    private final static String PFX = "com.mchange.v2.cfg.junit.mlogbackstop";

    /** Named so that nothing in any shipped or test configuration sets it. */
    private final static String UNSET_KEY = PFX + ".unsetKey";

    /**
     *  Set by the scenario's /mchange-commons.properties, which is in the hardcoded source list
     *  mlogUnified() returns. A key the RESOURCES supply is what a backstop has to lose to.
     */
    private final static String LIBRARY_SET_KEY = "layered.key";

    private Properties saved;

    @Override
    protected void setUp()
    {
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
    }

    @Override
    protected void tearDown()
    {
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( k.startsWith( PFX ) ) System.setProperty( k, saved.getProperty( k ) );
    }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( k.startsWith( PFX ) ) doomed.add( k );
        for ( String k : doomed ) System.clearProperty( k );
    }

    // ------------------------------------------------------------- the two ends

    /** A backstop must supply a value where nothing else does, or it does nothing at all. */
    public void testABackstopSuppliesAnOtherwiseUnsetKey()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();

            assertNull( "precondition: nothing should set " + UNSET_KEY, d.getProperty( UNSET_KEY ) );

            d.refreshWithBackstops( new Object[] { d.override( UNSET_KEY, "fromBackstop" ) }, "backstop only" );

            assertEquals( "fromBackstop", d.getProperty( UNSET_KEY ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  And must NOT displace a value the configuration already supplies. That is the whole
     *  distinction from an override, and the half that an override would fail.
     */
    public void testABackstopLosesToTheResourceConfiguration()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();

            String configured = d.getProperty( LIBRARY_SET_KEY );
            assertNotNull( "precondition: " + LIBRARY_SET_KEY + " should come from the scenario's "
                               + "/mchange-commons.properties. If this is null the fixture is broken, "
                               + "not the behavior.", configured );

            d.refreshWithBackstops( new Object[] { d.override( LIBRARY_SET_KEY, "BACKSTOP_SHOULD_LOSE" ) },
                                    "losing backstop" );

            assertEquals( "a backstop must not displace configuration that already spoke",
                          configured, d.getProperty( LIBRARY_SET_KEY ) );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------- against the other layers

    /** Where both programmatic layers claim a key, the override wins. */
    public void testOverridesBeatBackstops()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();

            d.refresh( new Object[] { d.override( UNSET_KEY, "fromBackstop" ) }, "bs",
                       new Object[] { d.override( UNSET_KEY, "fromOverride" ) }, "ov" );

            assertEquals( "fromOverride", d.getProperty( UNSET_KEY ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** And a backstop still applies to keys the overrides do not mention. */
    public void testBackstopsAndOverridesComposeRatherThanReplace()
    {
        String otherKey = UNSET_KEY + "2";

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();

            d.refresh( new Object[] { d.override( otherKey, "fromBackstop" ) }, "bs",
                       new Object[] { d.override( UNSET_KEY, "fromOverride" ) }, "ov" );

            assertEquals( "fromOverride", d.getProperty( UNSET_KEY ) );
            assertEquals( "supplying overrides must not suppress the backstops",
                          "fromBackstop", d.getProperty( otherKey ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  THE ONE THAT CONSTRAINS THE CAPPING GATES. A backstop must lose to a System property, and
     *  to the sealed snapshot of one -- the same earliest-or-narrowest policy the override cases
     *  pin from above.
     *
     *  <p>This is also the case that notices if the gates in <code>refresh(..)</code> forget
     *  backstops. They read <code>!mlogInitialized || backstops != null || overrides != null</code>,
     *  choosing between capping the resource list early and capping once at the end, after the
     *  programmatic layers. Either way a System property must beat a backstop; what the gate
     *  decides is whether the system-property layers are read once or twice.</p>
     */
    public void testABackstopLosesToASystemProperty()
    {
        System.setProperty( UNSET_KEY, "from-system" );

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();

            d.refreshWithBackstops( new Object[] { d.override( UNSET_KEY, "fromBackstop" ) },
                                    "backstop against a system property" );

            assertEquals( "a System property is the operator's word and outranks a fallback",
                          "from-system", d.getProperty( UNSET_KEY ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  A refresh carrying only backstops must still be reported as a refresh, and must say which
     *  layer it carried. The notice is how an operator confirms their configuration arrived; one
     *  that said "without backstops or overrides" after being handed backstops would be worse
     *  than silence.
     */
    public void testTheNoticeNamesTheBackstops()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();
            List<String[]> logged = d.installRecordingClassLogger();

            d.refreshWithBackstops( new Object[] { d.override( UNSET_KEY, "fromBackstop" ) }, "my backstops" );
            d.logDelayedItems();

            String all = MLogConfigDriver.describe( logged );
            assertTrue( "the notice should say backstops were supplied: " + all,
                        all.contains( "with backstops specified" ) );
            assertTrue( "and should carry their description: " + all, all.contains( "my backstops" ) );
            assertFalse( "it must not claim overrides were supplied: " + all,
                         all.contains( "with overrides specified" ) );
        }
        finally
        { s.closeQuietly(); }
    }
}
