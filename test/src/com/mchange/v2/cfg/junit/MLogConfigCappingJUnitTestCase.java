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
     *  THE REASON FOR THE EXCEPTION. A first load must not reach the snapshot, because
     *  reaching it logs and the first load is MLog coming up.
     */
    public void testAFirstLoadDoesNotSeal()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            assertFalse( "precondition: a fresh scenario starts unsealed", d.isSealed() );

            d.refresh( null, null );

            assertFalse( "MLog's own bootstrap must not seal System properties", d.isSealed() );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  And it does not need to: the live "/" is already the last element of MLogConfig's source
     *  list, so System properties win on the first load without being capped.
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
            assertFalse( "and it still must not have sealed", d.isSealed() );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------- later refreshes

    /** Once MLog is up, capping is safe, and a later refresh takes it. */
    public void testALaterRefreshSeals()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );
            assertFalse( d.isSealed() );

            d.refresh( null, null );

            assertTrue( "a refresh after the first load caps, and so seals", d.isSealed() );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  And the cap means what it means: the snapshot sits above a live read, so a write after
     *  sealing cannot change what logging is configured with.
     */
    public void testALaterRefreshCapsWithTheSnapshotRatherThanTheLiveValue()
    {
        System.setProperty( KEY, "before" );

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.refresh( null, null );   // first load: no seal
            d.refresh( null, null );   // caps, and seals with KEY=before

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
}
