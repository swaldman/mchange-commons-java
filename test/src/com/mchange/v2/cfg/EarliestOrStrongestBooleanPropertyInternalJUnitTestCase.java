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
