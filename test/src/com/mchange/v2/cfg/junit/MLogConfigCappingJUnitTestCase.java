package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *  MLogConfig caps its resolved configuration with live System properties and then the sealed
 *  snapshot above them, the same way c3p0 does -- but <b>never on the first load</b>.
 *
 *  <p>The exception is not an optimization. Reaching the snapshot calls
 *  SealedSystemProperties.get(), which logs, and MLog's own class initializer is what calls
 *  MLogConfig.refresh(null, null) in the first place. Capping there would ask the logging
 *  library to log while it was still deciding how to log. Nothing is sealed that early anyway,
 *  and the live "/" already sits at the end of MLogConfig's source list, so System properties
 *  have the precedence they should have on the first load without the snapshot's help.</p>
 *
 *  <p>So there are two regimes to pin, and the boundary between them: that a first load does
 *  not seal, that a later refresh does and caps, and that in both regimes a System property
 *  outranks whatever the refresh brought in.</p>
 *
 *  <h3>Why it is driven this way</h3>
 *
 *  <p>Through a CfgScenario, for two reasons that compound. MLogConfig's {@code config} is a
 *  static and "first load" means that static is null, so only a fresh ClassLoader can produce a
 *  first load more than once. And SealedSystemProperties is a one-shot static too -- a scenario
 *  gives each case an unsealed one, which is the only way to observe sealing happening.</p>
 *
 *  <p>MLogConfig.refresh is called directly rather than through MLog, because initializing MLog
 *  would itself perform the first load, and asynchronously: the MLog-Init-Reporter thread and
 *  the test would race over it. System properties are deliberately NOT reloaded by the scenario
 *  ClassLoader -- they live in java.base -- so writes here are visible to the scenario's reads,
 *  which is what makes any of this observable.</p>
 */
public final class MLogConfigCappingJUnitTestCase extends TestCase
{
    /** No resource-path text files, so the dispatch takes the hardcoded-source-list branch. */
    private final static String SCENARIO = "no-pathfiles";

    private final static String PFX = "com.mchange.v2.cfg.junit.mlogcap";
    private final static String KEY = PFX + ".key";

    private Properties saved;

    @Override
    protected void setUp() throws Exception
    {
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
    }

    @Override
    protected void tearDown() throws Exception
    {
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                System.setProperty( k, saved.getProperty( k ) );
    }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( k.startsWith( PFX ) ) doomed.add( k );
        for ( String k : doomed ) System.clearProperty( k );
    }

    // ------------------------------------------------------- the first load

    /**
     *  EVERY load seals, the first one included. There used to be an exception: reaching the
     *  snapshot logged, and the first load is MLog coming up, so a bootstrap load deliberately
     *  skipped the cap and read the live "/" instead.
     *
     *  <p>That exception is gone, because its cause is gone. Everything on the load path now
     *  reports through delayed log items -- SealedSystemProperties' init* methods accumulate
     *  rather than log, and SealedSystemPropertiesConfigSource reports through its Parse -- so
     *  nothing on the path calls a logger, and the cap can apply uniformly.</p>
     *
     *  <p>Sealing earlier is also the better behaviour on its own terms: the snapshot is the
     *  operator's word, and the less application code runs before it is taken, the more nearly
     *  that is true.</p>
     */
    public void testEvenTheFirstLoadSeals()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            assertFalse( "precondition: a fresh scenario starts unsealed", d.isSealed() );

            d.refresh( null, null );

            assertTrue( "the bootstrap load caps, and so seals", d.isSealed() );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  System properties are top of the ladder on the first load, as on every other. This held
     *  before the cap became unconditional too, by a different route -- the live "/" was already
     *  the last element of the source list -- so the assertion is unchanged while the reason for
     *  it is not.
     */
    public void testAFirstLoadStillGivesSystemPropertiesTopPrecedence()
    {
        System.setProperty( KEY, "from-system" );

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );

            assertEquals( "from-system", d.getProperty( KEY ) );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------- what the cap is for

    /**
     *  Sealing does not wait for MLog, and does not need to be asked twice. This is the case that
     *  used to turn on the "is MLog initialized" predicate; with nothing on the load path logging,
     *  that predicate no longer has anything to protect and MLogConfig no longer consults it.
     *
     *  <p>Driven with MLog never coming up at all, which is the configuration that used to be
     *  required to stay unsealed and is now the sharpest demonstration that the seal is
     *  unconditional.</p>
     */
    public void testSealingDoesNotWaitForMLog()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );

            d.refresh( null, null );
            assertTrue( "the first load seals, with MLog still down", d.isSealed() );

            // THE INVARIANT THE init* FAMILY EXISTS TO PRESERVE, asserted directly. Capping now
            // reads SEALED_SYSTEM_PROPERTIES on the bootstrap load, and if anything on that path
            // logs -- a convenience wrapper that evaluates logger(), say -- MLog comes up in the
            // middle of a config load, re-enters refresh(..), and its init reporter drains the
            // delayed items out from under the caller. That is exactly what happened when this
            // simplification was first attempted, and it surfaced only as a puzzling failure in
            // MLogDelayedItemLoggingJUnitTestCase. Here it is a direct assertion.
            assertFalse( "nothing on the load path may initialize MLog", d.isMLogInitialized() );

            d.refresh( null, null );
            assertTrue( "and a second load leaves it sealed", d.isSealed() );
            assertFalse( "still not, on a second load", d.isMLogInitialized() );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  WHAT THE CAP IS FOR, and the whole point of sealing: the snapshot sits above a live read,
     *  so a write to System properties after the seal cannot change what logging is configured
     *  with. An application that reconfigures logging by setting a System property at runtime is
     *  doing the thing the snapshot exists to refuse.
     *
     *  <p>Now that the first load seals, this needs no preparation: one load takes the snapshot,
     *  and every later read is capped by it.</p>
     */
    public void testARefreshCapsWithTheSnapshotRatherThanTheLiveValue()
    {
        System.setProperty( KEY, "before" );

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );                  // seals, with KEY=before
            assertEquals( "precondition: the first load saw it", "before", d.getProperty( KEY ) );

            System.setProperty( KEY, "after" );
            d.refresh( null, null );

            assertEquals( "the snapshot must cap the live read", "before", d.getProperty( KEY ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  The case c3p0 hit, in MLog's own terms. With overrides the config is a combine, and
     *  overrides sit after the source list in it -- so without capping, a refresh could
     *  override the operator's -D logging settings.
     */
    public void testAnOverrideCannotOutrankASystemPropertyOnALaterRefresh()
    {
        System.setProperty( KEY, "from-system" );

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );
            d.initializeMLog();
            d.refresh( new Object[] { d.override( KEY, "from-override" ) }, "capping test" );

            assertEquals( "from-system", d.getProperty( KEY ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  The complement, and worth as much: where no System property claims the key, the override
     *  must still take effect. A cap that silenced refreshes outright would pass every other
     *  case here.
     */
    public void testAnOverrideStillWinsWhereNoSystemPropertyClaimsTheKey()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );
            d.refresh( new Object[] { d.override( KEY, "from-override" ) }, "capping test" );

            assertEquals( "from-override", d.getProperty( KEY ) );
        }
        finally
        { s.closeQuietly(); }
    }
    /**
     *  The other dispatch branch caps on the first load too. The two branches read different
     *  source lists -- the path-file mechanism's, and the hardcoded one -- and each appends the
     *  current-then-sealed System properties itself, so each had to be checked separately when
     *  the cap was conditional, and still has to be now that it is not.
     */
    public void testThePathFileBranchSealsToo()
    {
        CfgScenario s = CfgScenario.open( "all-three-pathfiles", false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );

            d.refresh( null, null );
            assertTrue( "the path-file branch caps on its first load as well", d.isSealed() );
        }
        finally
        { s.closeQuietly(); }
    }

}
