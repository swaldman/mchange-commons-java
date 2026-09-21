package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 *  SystemOnlyStrengthensBooleanProperty: the intermediate position between the ratchet and the
 *  sealed pair.
 *
 *  <p>System properties may tighten a flag at runtime but never loosen it, so the only state
 *  retained is whether they have ever asserted the safe value. Application configuration is
 *  honored per call, in both directions, as with the sealed implementations.</p>
 *
 *  <p>The one thing this buys over simply sealing is recovery from a premature seal: a
 *  deployment that sets a flag safe after the snapshot was taken is still heard. Whether that
 *  is worth a second policy for readers to learn is an open question -- symmetry with the
 *  whitelist decision argues for retiring this class -- so these tests pin what it does while
 *  it exists.</p>
 */
public class SystemOnlyStrengthensBooleanPropertyInternalJUnitTestCase extends TestCase
{
    private final static String PFX     = "com.mchange.v2.cfg.junit.sosb";
    private final static String PERMIT  = PFX + ".permit";    // safe when false
    private final static String ENFORCE = PFX + ".enforce";   // safe when true

    private CapturingMLogger logger;
    private Properties saved;

    @Override
    public void setUp() throws Exception
    {
        logger = new CapturingMLogger();
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
        SecurityRatchetTestSupport.unsealSystemProperties();
    }

    @Override
    public void tearDown() throws Exception
    {
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                System.setProperty( k, saved.getProperty( k ) );
        SecurityRatchetTestSupport.unsealSystemProperties();
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

    private SystemOnlyStrengthensBooleanProperty permitFlag()
    { return new SystemOnlyStrengthensBooleanProperty( PERMIT, false, true ); }

    private boolean value( SystemOnlyStrengthensBooleanProperty p )
    { return p.getValue( null, logger ); }

    private boolean value( SystemOnlyStrengthensBooleanProperty p, PropertiesConfig cfg )
    { return p.getValue( cfg, logger ); }

    // ==================== System properties: strengthen only ====================

    /** The capability this class has and the sealed implementation does not. */
    public void testARuntimeSyspropMayTighten()
    {
        SystemOnlyStrengthensBooleanProperty p = permitFlag();
        assertTrue( "Precondition: permissive by default.", value( p ) );

        System.setProperty( PERMIT, "false" );

        assertFalse( "A System property asserting the safe value is honored, even late.", value( p ) );
    }

    public void testARuntimeSyspropMayNotLoosen()
    {
        System.setProperty( PERMIT, "false" );
        SystemOnlyStrengthensBooleanProperty p = permitFlag();
        assertFalse( value( p ) );

        System.setProperty( PERMIT, "true" );

        assertFalse( "A System property must never loosen a flag.", value( p ) );
    }

    /** and the tightening is permanent -- removing the property does not undo it. */
    public void testASyspropTighteningIsIrreversible()
    {
        SystemOnlyStrengthensBooleanProperty p = permitFlag();
        System.setProperty( PERMIT, "false" );
        assertFalse( value( p ) );

        System.clearProperty( PERMIT );

        assertFalse( value( p ) );
    }

    /** A sealed safe value counts as an assertion, even if the property is gone by first read. */
    public void testASealedSafeValueTightensEvenIfLaterRemoved()
    {
        System.setProperty( PERMIT, "false" );
        SealedSystemProperties.seal();
        System.clearProperty( PERMIT );

        assertFalse( value( permitFlag() ) );
    }

    // ==================== application config: honored per call ====================

    public void testConfigIsHonoredPerCallInBothDirections()
    {
        SystemOnlyStrengthensBooleanProperty p = permitFlag();

        assertFalse( value( p, pcfg( PERMIT, "false" ) ) );
        assertTrue( "Config may loosen again -- only System properties ratchet.",
                    value( p, pcfg( PERMIT, "true" ) ) );
    }

    public void testConfigInfluenceDoesNotOutliveTheCall()
    {
        SystemOnlyStrengthensBooleanProperty p = permitFlag();

        assertFalse( value( p, pcfg( PERMIT, "false" ) ) );
        assertTrue( "and withdrawing it restores the default.", value( p ) );
    }

    /**
     *  But config cannot undo what System properties asserted. The ratchet is on the System
     *  dimension, and nothing below it may raise the floor back up.
     */
    public void testConfigCannotLoosenWhatASyspropTightened()
    {
        System.setProperty( PERMIT, "false" );
        SystemOnlyStrengthensBooleanProperty p = permitFlag();
        assertFalse( value( p ) );

        assertFalse( "Application config must not reopen what the operator closed.",
                     value( p, pcfg( PERMIT, "true" ) ) );
    }

    // ==================== defaults and notices ====================

    public void testUnconfiguredWithAnUnsafeDefaultSaysSoOnceThenReArms()
    {
        SystemOnlyStrengthensBooleanProperty p =
            new SystemOnlyStrengthensBooleanProperty( ENFORCE, true, false );

        assertFalse( value( p ) );
        assertEquals( "Exactly one nag: " + logger.warnings(),
                      1, logger.warningsContaining( "less secure value" ).size() );

        logger.clear();
        assertFalse( value( p ) );
        assertTrue( "silent while nothing changes: " + logger.warnings(), logger.warnings().isEmpty() );

        assertFalse( "now explicitly configured", value( p, pcfg( ENFORCE, "false" ) ) );
        logger.clear();

        assertFalse( "and unconfigured again", value( p ) );
        assertEquals( "The notice re-arms for a newly unconfigured flag: " + logger.warnings(),
                      1, logger.warningsContaining( "less secure value" ).size() );
    }

    public void testUnconfiguredWithASafeDefaultIsSilent()
    {
        SystemOnlyStrengthensBooleanProperty p =
            new SystemOnlyStrengthensBooleanProperty( ENFORCE, true );

        assertTrue( value( p ) );
        assertTrue( "A safe default needs no comment: " + logger.warnings(), logger.warnings().isEmpty() );
    }
}
