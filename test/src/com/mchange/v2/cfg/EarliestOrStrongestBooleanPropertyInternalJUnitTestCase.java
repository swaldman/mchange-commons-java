package com.mchange.v2.cfg;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 *  EarliestOrStrongestBooleanProperty: the boolean counterpart of the whitelist ratchet.
 *
 *  <p>Each security-sensitive flag has a safe polarity -- an enforcement switch is safe when
 *  true, a permission switch is safe when false -- and the rule is the same as for
 *  whitelists. The sealed snapshot of System properties is the baseline; later configuration
 *  may move the flag <i>toward</i> safety but never away from it; and once the safe value is
 *  reached it latches, because privilege under a ratchet never increases.</p>
 *
 *  <p>The default matters separately from the polarity. A flag that already defaults to its
 *  safe value needs no comment. One that defaults to the less safe value -- because tightening
 *  it would break existing deployments -- says so, once, and says that the default may change.
 *  That is the treatment ByNameInstantiationUtils currently hand-rolls for
 *  enforceWhitelist.</p>
 *
 *  <p>Declared in-package: these need BasicMultiPropertiesConfig, and they need to reset the
 *  one-shot seal between cases, as SealedSystemPropertiesInternalJUnitTestCase explains.
 *  Ordering matters within each case -- a property must be set <i>before</i> the first
 *  getValue call if it is to appear in the sealed snapshot.</p>
 */
public class EarliestOrStrongestBooleanPropertyInternalJUnitTestCase extends TestCase
{
    private final static String PFX     = "com.mchange.v2.cfg.junit.eosb";
    private final static String ENFORCE = PFX + ".enforce";   // safe when true
    private final static String PERMIT  = PFX + ".permit";    // safe when false

    private CapturingMLogger logger;
    private Properties saved;

    @Override
    public void setUp() throws Exception
    {
        logger = new CapturingMLogger();
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
        unseal();
    }

    @Override
    public void tearDown() throws Exception
    {
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                System.setProperty( k, saved.getProperty( k ) );
        unseal();
    }

    private static void unseal() throws Exception
    {
        Field f = SealedSystemProperties.class.getDeclaredField( "theProperties" );
        f.setAccessible( true );
        f.set( null, null );
    }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                doomed.add( k );
        for ( String k : doomed )
            System.clearProperty( k );
    }

    private static PropertiesConfig pcfg( String key, String value )
    {
        Properties p = new Properties();
        p.setProperty( key, value );
        return new BasicMultiPropertiesConfig( "/notional-test-resource", p );
    }

    /** an enforcement switch: safe when true, defaulting to the less safe false */
    private EarliestOrStrongestBooleanProperty enforceFlag()
    { return new EarliestOrStrongestBooleanProperty( ENFORCE, true, false ); }

    /** a permission switch: safe when false, defaulting to the less safe true */
    private EarliestOrStrongestBooleanProperty permitFlag()
    { return new EarliestOrStrongestBooleanProperty( PERMIT, false, true ); }

    private boolean value( EarliestOrStrongestBooleanProperty p )
    { return p.getValue( null, logger ); }

    private boolean value( EarliestOrStrongestBooleanProperty p, PropertiesConfig pcfg )
    { return p.getValue( pcfg, logger ); }

    // ==================== defaults ====================

    /** A flag whose default is already its safe value has nothing to warn about. */
    public void testUnconfiguredWithASafeDefaultIsSilent()
    {
        EarliestOrStrongestBooleanProperty p =
            new EarliestOrStrongestBooleanProperty( ENFORCE, true );   // default == strongest

        assertTrue( value( p ) );
        assertTrue( "A safe default needs no comment: " + logger.warnings(), logger.warnings().isEmpty() );
    }

    /**
     *  One whose default is the less safe value says so, and says the default may change --
     *  the deployment is running on an allowance, not a decision.
     */
    public void testUnconfiguredWithAnUnsafeDefaultSaysSoOnce()
    {
        EarliestOrStrongestBooleanProperty p = enforceFlag();

        assertFalse( value( p ) );
        assertEquals( "Exactly one nag: " + logger.warnings(),
                      1, logger.warningsContaining( "less secure value" ).size() );
        assertTrue( "and it must warn that the default may change.",
                    logger.sawWarningContaining( "MAY CHANGE" ) );

        logger.clear();
        assertFalse( value( p ) );
        assertTrue( "The nag must not repeat on every lookup: " + logger.warnings(),
                    logger.warnings().isEmpty() );
    }

    /** An explicit weak setting is a choice, not an allowance, and is not nagged about. */
    public void testExplicitlyConfiguredUnsafeValueIsNotNagged()
    {
        System.setProperty( ENFORCE, "false" );

        assertFalse( value( enforceFlag() ) );
        assertTrue( "An explicit choice must not draw the default nag: " + logger.warnings(),
                    logger.warningsContaining( "less secure value" ).isEmpty() );
    }

    // ==================== the sealed baseline ====================

    public void testSafeValueInTheSealedSnapshotIsHonored()
    {
        System.setProperty( ENFORCE, "true" );

        assertTrue( value( enforceFlag() ) );
    }

    /**
     *  "Earliest": a safe value captured in the snapshot survives being weakened afterward.
     *  This is the case the seal exists for -- the flag cannot be turned off at runtime.
     */
    public void testASafeSealedValueCannotBeWeakenedLater()
    {
        System.setProperty( ENFORCE, "true" );
        EarliestOrStrongestBooleanProperty p = enforceFlag();
        assertTrue( value( p ) );

        System.setProperty( ENFORCE, "false" );

        assertTrue( "A sealed safe value must not be undone.", value( p ) );
    }

    /**
     *  The case that isolates the snapshot from the latch. Both of the cases above would
     *  pass with no snapshot at all, because the first read latches the safe value and the
     *  latch alone then resists weakening. Here the flag is weakened <i>before</i> the first
     *  read, so only the sealed baseline can still report it safe -- which is precisely the
     *  attack the seal exists for: set the flag off before anything consults it.
     */
    public void testASafeValueWeakenedBeforeTheFirstReadIsStillHonored()
    {
        System.setProperty( ENFORCE, "true" );
        SealedSystemProperties.seal();
        System.setProperty( ENFORCE, "false" );

        assertTrue( "The sealed baseline must decide, not the live value.", value( enforceFlag() ) );
    }

    /** and it holds even if the property is removed entirely. */
    public void testASafeSealedValueSurvivesRemovalOfTheProperty()
    {
        System.setProperty( ENFORCE, "true" );
        EarliestOrStrongestBooleanProperty p = enforceFlag();
        assertTrue( value( p ) );

        System.clearProperty( ENFORCE );

        assertTrue( value( p ) );
    }

    // ==================== "or strongest": later tightening is honored ====================

    public void testStrengtheningViaSyspropsAfterSealingIsHonored()
    {
        System.setProperty( ENFORCE, "false" );
        EarliestOrStrongestBooleanProperty p = enforceFlag();
        assertFalse( value( p ) );

        System.setProperty( ENFORCE, "true" );

        assertTrue( "A change toward safety is honored even after sealing.", value( p ) );
    }

    public void testStrengtheningViaPropertiesConfigIsHonored()
    {
        EarliestOrStrongestBooleanProperty p = enforceFlag();
        assertFalse( value( p, pcfg( ENFORCE, "false" ) ) );

        assertTrue( value( p, pcfg( ENFORCE, "true" ) ) );
    }

    /** and having been strengthened, it cannot be relaxed again. */
    public void testAStrengthenedFlagCannotBeWeakened()
    {
        System.setProperty( ENFORCE, "false" );
        EarliestOrStrongestBooleanProperty p = enforceFlag();
        value( p );

        System.setProperty( ENFORCE, "true" );
        assertTrue( value( p ) );

        System.setProperty( ENFORCE, "false" );
        assertTrue( "The ratchet must not turn back.", value( p ) );

        System.clearProperty( ENFORCE );
        assertTrue( "nor be released by removing the property.", value( p ) );
    }

    /** A flag that is never strengthened simply stays where it was, without comment. */
    public void testAFlagThatIsNeverStrengthenedStaysWeakAndQuiet()
    {
        System.setProperty( ENFORCE, "false" );
        EarliestOrStrongestBooleanProperty p = enforceFlag();
        assertFalse( value( p ) );
        logger.clear();

        assertFalse( value( p ) );
        assertFalse( value( p ) );

        assertTrue( "Steady state must be silent: " + logger.warnings(), logger.warnings().isEmpty() );
    }

    // ==================== removal is a tightening ====================

    /**
     *  Deleting a permissive setting reverts the flag to its default, and when that default is
     *  the safe value the deletion is a tightening -- so it is honored, exactly as removing a
     *  whitelist key narrows a whitelist. Requiring an operator to write "false" rather than
     *  delete the line would be a surprising rule to have to know, and the gesture they will
     *  actually make when revoking a dangerous opt-in is to remove it.
     */
    public void testRemovingAPermissiveSettingTightensToTheSafeDefault()
    {
        EarliestOrStrongestBooleanProperty p = permitFlag();   // safe when false, defaults true
        System.setProperty( PERMIT, "false" );
        assertFalse( "Precondition: explicitly safe.", value( p ) );

        // and the mirror: a flag configured permissively, then unconfigured
        System.setProperty( ENFORCE, "false" );                // safe when true, so false is weak
        EarliestOrStrongestBooleanProperty q =
            new EarliestOrStrongestBooleanProperty( ENFORCE, true, true );   // default is the safe value
        assertFalse( "Precondition: explicitly configured to the weak value.", value( q ) );

        System.clearProperty( ENFORCE );

        assertTrue( "Removing the weak setting must revert to the safe default.", value( q ) );
    }

    /**
     *  The sealed snapshot must not defeat that. It retains the old permissive value for the
     *  life of the JVM, so a removal test that asks "is this unconfigured?" of the snapshot as
     *  well as of live configuration can never see the removal at all.
     */
    public void testRemovalIsSeenDespiteThePermissiveValueSurvivingInTheSnapshot()
    {
        System.setProperty( ENFORCE, "false" );
        EarliestOrStrongestBooleanProperty p =
            new EarliestOrStrongestBooleanProperty( ENFORCE, true, true );
        assertFalse( value( p ) );
        assertTrue( "Precondition: the snapshot holds the weak value.",
                    "false".equals( SealedSystemProperties.get().getProperty( ENFORCE ) ) );

        System.clearProperty( ENFORCE );

        assertTrue( "A stale snapshot entry must not mask the removal.", value( p ) );
    }

    /** Removal tightens, but the tightening is itself permanent -- the ratchet does not reopen. */
    public void testAFlagTightenedByRemovalCannotBeLoosenedAgain()
    {
        System.setProperty( PERMIT, "true" );                  // permissive, and it is the default
        EarliestOrStrongestBooleanProperty p = permitFlag();
        assertTrue( "Precondition: permissive.", value( p ) );

        System.clearProperty( PERMIT );
        assertTrue( "A default that is itself permissive stays permissive on removal.", value( p ) );

        System.setProperty( PERMIT, "false" );
        assertFalse( value( p ) );

        System.clearProperty( PERMIT );
        assertFalse( "Having reached safety, removal must not relax it.", value( p ) );

        System.setProperty( PERMIT, "true" );
        assertFalse( "nor may re-asserting the permissive value.", value( p ) );
    }

    /**
     *  Removal reverts to the <i>default</i>, not to the safe value. A flag whose default is
     *  permissive becomes permissive again -- unconfigured means unconfigured, and it is only
     *  a tightening when the default happens to be the safe side.
     */
    public void testRemovalRevertsToTheDefaultRatherThanToSafety()
    {
        System.setProperty( PERMIT, "true" );
        EarliestOrStrongestBooleanProperty p = permitFlag();   // safe false, default true
        assertTrue( value( p ) );

        System.clearProperty( PERMIT );

        assertTrue( "The default is permissive, so removal cannot make it safe.", value( p ) );
    }

    // ==================== the invariant, exhaustively ====================

    /**
     *  The property that everything else is in service of: the value never moves from the safe
     *  side to the less safe side, whatever an operator does to configuration afterward.
     *
     *  <p>This exercises it directly rather than case by case -- every sequence of set-true,
     *  set-false and clear up to length five, over every combination of polarity and default,
     *  with the sealed snapshot reset between sequences so each begins as a fresh JVM would.
     *  The individual cases above pin the behaviors that <i>imply</i> the invariant; this pins
     *  the invariant, which is what should survive a refactoring that reorganizes them.</p>
     *
     *  <p>Where the safety actually comes from is worth knowing: configureUnconfigured, the
     *  only path that can lower a value, is reachable only from inside the guard that skips a
     *  flag already at its safe value. Relax that guard -- treat it as the mere optimization it
     *  resembles -- and removal of configuration would loosen a safe flag. This test is the one
     *  that would notice.</p>
     */
    public void testTheValueNeverMovesFromSafeToLessSafe() throws Exception
    {
        final String[] gestures = { "set-true", "set-false", "clear" };
        int sequences = 0, tightenings = 0;

        for ( boolean strongest : new boolean[]{ true, false } )
            for ( boolean dflt : new boolean[]{ true, false } )
                for ( int len = 1; len <= 5; ++len )
                    for ( int code = 0, n = (int) Math.pow( 3, len ); code < n; ++code )
                    {
                        List<String> seq = new ArrayList<String>();
                        for ( int i = 0, c = code; i < len; ++i, c /= 3 )
                            seq.add( gestures[ c % 3 ] );

                        System.clearProperty( ENFORCE );
                        unseal();   // each sequence starts as a fresh JVM would
                        EarliestOrStrongestBooleanProperty p =
                            new EarliestOrStrongestBooleanProperty( ENFORCE, strongest, dflt );

                        Boolean prev = null;
                        for ( String g : seq )
                        {
                            if ( "clear".equals( g ) ) System.clearProperty( ENFORCE );
                            else System.setProperty( ENFORCE, "set-true".equals( g ) ? "true" : "false" );

                            boolean v = p.getValue( null, logger );
                            if ( prev != null )
                            {
                                boolean wasSafe = ( prev.booleanValue() == strongest );
                                boolean nowSafe = ( v == strongest );
                                assertFalse( "Moved from safe to less safe: strongest=" + strongest +
                                             " default=" + dflt + " " + seq,
                                             wasSafe && !nowSafe );
                                if ( !wasSafe && nowSafe ) ++tightenings;
                            }
                            prev = Boolean.valueOf( v );
                        }
                        ++sequences;
                    }

        assertTrue( "Precondition: the sweep must actually be exercising something.", sequences > 1000 );
        assertTrue( "and must include tightenings, or it proves only that nothing ever changes.",
                    tightenings > 0 );
    }

    // ==================== the other polarity ====================

    /**
     *  A permission switch is safe when false, so the ratchet runs the other way. Nothing in
     *  the mechanism should privilege 'true' as the safe answer.
     */
    public void testAFalseBiasedFlagRatchetsTowardFalse()
    {
        EarliestOrStrongestBooleanProperty p = permitFlag();

        assertTrue( "Unconfigured, it defaults to the permissive value.", value( p ) );

        assertFalse( "Disabling the permission is a tightening, and is honored.",
                     value( p, pcfg( PERMIT, "false" ) ) );

        assertFalse( "Re-enabling it is a loosening, and is not.",
                     value( p, pcfg( PERMIT, "true" ) ) );
    }

    public void testAFalseBiasedFlagSealedSafeCannotBeReopened()
    {
        System.setProperty( PERMIT, "false" );
        EarliestOrStrongestBooleanProperty p = permitFlag();
        assertFalse( value( p ) );

        System.setProperty( PERMIT, "true" );

        assertFalse( value( p ) );
    }

    // ==================== parsing ====================

    public void testValuesAreCaseInsensitiveAndTrimmed()
    {
        System.setProperty( ENFORCE, "  TRUE  " );

        assertTrue( value( enforceFlag() ) );
    }

    /**
     *  An uninterpretable value tells us nothing, so the flag falls back to its default --
     *  and, since a typo is easy and the consequence is a security setting not taking effect,
     *  says what it could not read.
     */
    public void testAnUninterpretableValueFallsBackToTheDefaultAndSaysSo()
    {
        System.setProperty( ENFORCE, "ture" );

        assertFalse( value( enforceFlag() ) );
        assertTrue( "The unreadable value must be reported: " + logger.warnings(),
                    logger.sawWarningContaining( "uninterpretable", "ture" ) );
    }

    /**
     *  and reports it once, not once per lookup. A flag at its weak value is re-read on every
     *  lookup -- which it must be, or a later strengthening could not be noticed -- so an
     *  unsuppressed parse warning would fire on every JNDI dereference or driver load.
     */
    public void testAnUninterpretableValueIsReportedOnceNotPerLookup()
    {
        System.setProperty( ENFORCE, "ture" );
        EarliestOrStrongestBooleanProperty p = enforceFlag();

        value( p );
        int afterFirst = logger.warningsContaining( "uninterpretable" ).size();
        value( p ); value( p );

        assertEquals( "One complaint per bad value, however many lookups: " + logger.warnings(),
                      afterFirst, logger.warningsContaining( "uninterpretable" ).size() );
    }

    /**
     *  The same bad value seen through the sealed snapshot and through live System properties
     *  is one operator mistake, and should read as one.
     */
    public void testOneBadSyspropIsReportedOnceNotOncePerView()
    {
        System.setProperty( ENFORCE, "ture" );

        value( enforceFlag() );

        assertEquals( "A single bad System property must not look like two problems: " + logger.warnings(),
                      1, logger.warningsContaining( "uninterpretable" ).size() );
    }

    /** whereas a bad value in configuration is a genuinely separate mistake. */
    public void testABadValueInEachSourceIsReportedSeparately()
    {
        System.setProperty( ENFORCE, "ture" );

        value( enforceFlag(), pcfg( ENFORCE, "flase" ) );

        assertTrue( logger.sawWarningContaining( "uninterpretable", "ture" ) );
        assertTrue( logger.sawWarningContaining( "uninterpretable", "flase" ) );
    }

    // ==================== the strengthening notice ====================

    public void testStrengtheningIsReportedAndNamesItsSource()
    {
        System.setProperty( ENFORCE, "false" );
        EarliestOrStrongestBooleanProperty p = enforceFlag();
        value( p );
        logger.clear();

        System.setProperty( ENFORCE, "true" );
        value( p );

        assertTrue( "A late tightening must be visible: " + logger.warnings(),
                    logger.sawWarningContaining( "updated to its strongest value" ) );
        assertTrue( "and must say where it came from.",
                    logger.sawWarningContaining( "updated to its strongest value", "system properties" ) );
    }

    /** A flag that was already safe at the first lookup has not been "updated", and is silent. */
    public void testAFlagSafeFromTheStartReportsNoUpdate()
    {
        System.setProperty( ENFORCE, "true" );

        assertTrue( value( enforceFlag() ) );

        assertTrue( "Nothing changed, so nothing to report: " + logger.warnings(),
                    logger.warningsContaining( "updated to its strongest value" ).isEmpty() );
    }
}
