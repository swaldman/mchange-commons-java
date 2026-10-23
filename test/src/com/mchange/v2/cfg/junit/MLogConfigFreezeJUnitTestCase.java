package com.mchange.v2.cfg.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 *  <code>com.mchange.v2.log.freezeConfig</code>: a deployment that sets it pins its logging
 *  configuration, and MLogConfig refuses every later refresh.
 *
 *  <p>The point is that logging configuration is a security control as much as a convenience --
 *  it decides what is recorded about a running system, so being able to replace it at runtime is
 *  a way to stop a system telling anyone what is happening to it. A deployment that has settled
 *  its logging and wants it to stay settled can say so.</p>
 *
 *  <h3>Why these look the way they do</h3>
 *
 *  <p>Each case runs in a CfgScenario, because MLogConfig's state is static and one-shot: a
 *  fresh ClassLoader per case is the only way to get more than one first load per JVM. System
 *  properties are deliberately NOT reloaded by the scenario loader -- they live in java.base --
 *  so writes here are visible to the scenario's reads, which is what makes a sealed property
 *  observable at all.</p>
 *
 *  <p>Every case that exercises the check calls initializeMLog() first, because the check is
 *  gated on MLog actually working, and for a reason: evaluating the flag reaches
 *  SealedSystemProperties.get(), which reads configuration and logs what it did. Run that while
 *  MLog is still coming up and it re-enters MLog.getLogger with no backend yet, the NPE escapes
 *  to MLog's static initializer, and logging silently falls back to System.err for the life of
 *  the ClassLoader. That is not hypothetical; it is what this feature cost to get right, and the
 *  reason the predicate is "is MLog up" rather than "is this a later load".</p>
 */
public final class MLogConfigFreezeJUnitTestCase extends TestCase
{
    /** No resource-path text files, so the dispatch takes the hardcoded-source-list branch. */
    private final static String SCENARIO = "no-pathfiles";

    private final static String FREEZE_KEY = "com.mchange.v2.log.freezeConfig";

    private final static String PFX = "com.mchange.v2.cfg.junit.mlogfreeze";
    private final static String KEY = PFX + ".key";

    private final static String REFUSAL = "has been frozen";
    private final static String NAG     = "less secure value";

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
            if ( k.startsWith( PFX ) || FREEZE_KEY.equals( k ) )
                System.setProperty( k, saved.getProperty( k ) );
    }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( k.startsWith( PFX ) || FREEZE_KEY.equals( k ) ) doomed.add( k );
        for ( String k : doomed ) System.clearProperty( k );
    }

    // ------------------------------------------------------------- refusing

    /**
     *  THE FEATURE. Frozen by a System property that was set before anything sealed, so the
     *  snapshot carries it: a later refresh is refused and its override never reaches config.
     */
    public void testAFrozenConfigRefusesARefresh()
    {
        System.setProperty( FREEZE_KEY, "true" );

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();
            List<String[]> logged = d.installRecordingClassLogger();

            d.refresh( new Object[] { d.override( KEY, "from-override" ) }, "freeze test" );

            assertNull( "a frozen config must not take the override: " + MLogConfigDriver.describe( logged ),
                        d.getProperty( KEY ) );
            assertEquals( "and the refusal must be reported at WARNING: " + MLogConfigDriver.describe( logged ),
                          1, MLogConfigDriver.warningsContaining( logged, REFUSAL ).size() );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  The complement, and worth as much: without the flag the very same refresh must succeed.
     *  A freeze that refused nothing, or an override that never arrived, would satisfy the case
     *  above for the wrong reason.
     */
    public void testAnUnfrozenConfigAcceptsTheSameRefresh()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();
            List<String[]> logged = d.installRecordingClassLogger();

            d.refresh( new Object[] { d.override( KEY, "from-override" ) }, "freeze test" );

            assertEquals( "from-override", d.getProperty( KEY ) );
            assertTrue( "nothing should have been refused: " + MLogConfigDriver.describe( logged ),
                        MLogConfigDriver.warningsContaining( logged, REFUSAL ).isEmpty() );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  A deployment that keeps calling refresh should keep being told it is being ignored -- the
     *  warning is how an operator finds out their refresh is not taking effect, so suppressing
     *  it after the first would hide the thing it exists to report. But the stack trace comes
     *  once: it identifies the caller, and the second caller is usually the same one.
     */
    public void testEveryRefusalIsReportedButOnlyTheFirstCarriesAStackTrace()
    {
        System.setProperty( FREEZE_KEY, "true" );

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();
            List<String[]> logged = d.installRecordingClassLogger();

            d.refresh( null, null );
            d.refresh( null, null );

            List<String[]> refusals = MLogConfigDriver.warningsContaining( logged, REFUSAL );
            assertEquals( "both refusals must be reported: " + MLogConfigDriver.describe( logged ),
                          2, refusals.size() );
            assertNotNull( "the first refusal should say who asked", refusals.get( 0 )[2] );
            assertNull( "the second need not repeat the stack trace", refusals.get( 1 )[2] );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------- not refusing the bootstrap

    /**
     *  A freeze must never prevent logging from configuring itself in the first place. The check
     *  is skipped entirely while MLog is down, so MLog's own initialization load proceeds even
     *  with the flag set -- otherwise setting it would leave a deployment with no logging
     *  configuration at all, which is a far worse outcome than the one it protects against.
     */
    public void testAFrozenConfigStillLoadsInTheFirstPlace()
    {
        System.setProperty( FREEZE_KEY, "true" );
        System.setProperty( KEY, "from-system" );

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();

            assertEquals( "MLog's own initialization load must not be refused",
                          "from-system", d.getProperty( KEY ) );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------- which config decides

    /**
     *  The decision is made under the config in force, NOT the one being proposed -- otherwise a
     *  refresh carrying freezeConfig=false would unfreeze itself and the control would be
     *  decorative.
     *
     *  <p>The corollary is worth pinning too, because it is mildly surprising: a refresh whose
     *  own overrides establish the freeze is NOT refused, since at the moment of the decision
     *  those overrides are not yet config. It takes effect from the next refresh onward.</p>
     */
    public void testFreezingTakesEffectFromTheRefreshAfterTheOneThatSetsIt()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();
            List<String[]> logged = d.installRecordingClassLogger();

            d.refresh( new Object[] { d.override( FREEZE_KEY, "true" ),
                                      d.override( KEY, "first" ) }, "establishes the freeze" );

            assertEquals( "the refresh that establishes the freeze is itself honored",
                          "first", d.getProperty( KEY ) );
            assertTrue( "and is not refused: " + MLogConfigDriver.describe( logged ),
                        MLogConfigDriver.warningsContaining( logged, REFUSAL ).isEmpty() );

            d.refresh( new Object[] { d.override( KEY, "second" ) }, "should be refused" );

            assertEquals( "but the next one is refused, so the first value stands",
                          "first", d.getProperty( KEY ) );
            assertEquals( "and the refusal is reported: " + MLogConfigDriver.describe( logged ),
                          1, MLogConfigDriver.warningsContaining( logged, REFUSAL ).size() );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  THE GUARANTEE THAT MAKES freezeConfig A SECURITY CONTROL RATHER THAN A PREFERENCE. Where
     *  the sealed snapshot and the configuration in force disagree, the SAFE answer wins -- so an
     *  operator who freezes cannot be overruled by the configuration of the application whose
     *  logging is being pinned.
     *
     *  <p>That is what the "strongest" argument to SealedSystemPropertiesBooleanProperty decides.
     *  Found by mutation: flipping it broke nothing, because a disagreement is harder to arrange
     *  than it looks. The decision reads the config IN FORCE, and that config already carries
     *  whatever the live System property said when it was loaded -- so simply setting the
     *  property and passing a contrary override has both sources agreeing, and arbitration never
     *  happens.</p>
     *
     *  <p>Hence the sequence below: load config while the property says false, so the config in
     *  force carries false; only then set it true, so the snapshot -- taken at this first
     *  evaluation -- carries true. Now they conflict, and frozen must win.</p>
     */
    public void testWhereTheSnapshotAndConfigDisagreeTheFreezeWins()
    {
        System.setProperty( FREEZE_KEY, "false" );

        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();                        // config in force now carries false
            assertFalse( "precondition: nothing sealed yet", d.isSealed() );
            assertEquals( "precondition: config in force says false", "false", d.getProperty( FREEZE_KEY ) );

            System.setProperty( FREEZE_KEY, "true" );  // the snapshot will capture true
            List<String[]> logged = d.installRecordingClassLogger();

            d.refresh( new Object[] { d.override( KEY, "from-override" ) }, "config says unfrozen" );

            assertNull( "the sealed freeze must beat the unfrozen config in force: "
                            + MLogConfigDriver.describe( logged ),
                        d.getProperty( KEY ) );
            assertEquals( "and the refusal must be reported: " + MLogConfigDriver.describe( logged ),
                          1, MLogConfigDriver.warningsContaining( logged, REFUSAL ).size() );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------- and quietly

    /**
     *  The reason AbstractBooleanProperty gained a warnUnconfiguredWeakDefault flag. Unfrozen is
     *  the settled default rather than a transitional one, so the "has not been set ... we
     *  default to the less secure value ... THIS MAY CHANGE IN A FUTURE RELEASE" notice would be
     *  asking every deployment, on every refresh, to act on something that is not going to
     *  change. Said that often it teaches readers to skim past the notices that mean something.
     */
    public void testAnUnsetFreezeFlagIsNotNaggedAbout()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            MLogConfigDriver d = new MLogConfigDriver( s );
            d.initializeMLog();
            List<String[]> logged = d.installRecordingClassLogger();

            d.refresh( null, null );
            d.refresh( null, null );

            assertTrue( "an unset freeze flag must say nothing: " + MLogConfigDriver.describe( logged ),
                        MLogConfigDriver.warningsContaining( logged, NAG ).isEmpty() );
        }
        finally
        { s.closeQuietly(); }
    }
}
