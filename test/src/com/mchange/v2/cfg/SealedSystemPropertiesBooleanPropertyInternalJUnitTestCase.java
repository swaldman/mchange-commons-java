package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 *  SealedSystemPropertiesBooleanProperty: System properties as of the seal, application
 *  configuration honored per call.
 *
 *  <p>The policy distinguishes the two sources by how they can be reached. System properties
 *  are ambient and writable by anything in the process, with no permission check since
 *  SecurityManager's removal, so they are read from the sealed snapshot and later mutation is
 *  invisible. A PropertiesConfig is a parameter the application chose to pass; honoring it per
 *  call is what the pcfg-taking overloads promise.</p>
 *
 *  <p>Where the two disagree the safe value wins -- the boolean counterpart of the whitelist's
 *  conservative intersection -- so an operator can pin a flag from the command line and
 *  application configuration cannot loosen it.</p>
 *
 *  <p>Ordering matters within each case: a property must be set <i>before</i> the first
 *  consultation if it is to appear in the snapshot. Each case unseals first, so it begins as a
 *  fresh JVM would.</p>
 */
public class SealedSystemPropertiesBooleanPropertyInternalJUnitTestCase extends TestCase
{
    private final static String PFX    = "com.mchange.v2.cfg.junit.sspb";
    private final static String PERMIT = PFX + ".permit";    // safe when false
    private final static String ENFORCE = PFX + ".enforce";  // safe when true

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

    /** a permission switch: safe when false, defaulting to the permissive true */
    private SealedSystemPropertiesBooleanProperty permitFlag()
    { return new SealedSystemPropertiesBooleanProperty( PERMIT, false, true ); }

    private boolean value( SealedSystemPropertiesBooleanProperty p )
    { return p.getValue( null, logger ); }

    private boolean value( SealedSystemPropertiesBooleanProperty p, PropertiesConfig cfg )
    { return p.getValue( cfg, logger ); }

    // ==================== application config, honored per call ====================

    /**
     *  The case that motivated the policy. An application opting into a capability by passing
     *  configuration must be honored -- and honored again, not only the first time.
     */
    public void testConfigOptInIsHonoredOnEveryCall()
    {
        SealedSystemPropertiesBooleanProperty p = permitFlag();

        assertTrue( "Precondition: permissive by default.", value( p ) );

        SealedSystemPropertiesBooleanProperty q =
            new SealedSystemPropertiesBooleanProperty( ENFORCE, true, false );
        assertFalse( "Precondition: an enforce flag starts off.", value( q, null ) );
        assertTrue( value( q, pcfg( ENFORCE, "true" ) ) );
        assertTrue( "and again -- the parameter is not consumed by its first use.",
                    value( q, pcfg( ENFORCE, "true" ) ) );
    }

    /**
     *  and an opt-in must not outlive the call that carried it. A pcfg is an argument, so its
     *  influence ends with the invocation; leaking it would make the gate depend on which
     *  earlier calls passed what.
     */
    public void testConfigInfluenceDoesNotOutliveTheCall()
    {
        SealedSystemPropertiesBooleanProperty p = permitFlag();

        assertFalse( "Configuration disables the capability.", value( p, pcfg( PERMIT, "false" ) ) );
        assertTrue( "and withdrawing that configuration restores the default.", value( p ) );
    }

    public void testConfigMayLoosenAsWellAsTightenWhenSyspropsAreSilent()
    {
        SealedSystemPropertiesBooleanProperty p = permitFlag();

        assertFalse( value( p, pcfg( PERMIT, "false" ) ) );
        assertTrue( "Nothing ratchets here: config decides each call.",
                    value( p, pcfg( PERMIT, "true" ) ) );
        assertFalse( value( p, pcfg( PERMIT, "false" ) ) );
    }

    // ==================== System properties, as of the seal ====================

    public void testSealedSyspropIsHonored()
    {
        System.setProperty( PERMIT, "false" );

        assertFalse( value( permitFlag() ) );
    }

    /** A runtime change to a System property is invisible -- it cannot loosen ... */
    public void testRuntimeSyspropLooseningIsInvisible()
    {
        System.setProperty( PERMIT, "false" );
        SealedSystemPropertiesBooleanProperty p = permitFlag();
        assertFalse( value( p ) );

        System.setProperty( PERMIT, "true" );

        assertFalse( "A later System property must not loosen the flag.", value( p ) );
    }

    /** ... and, symmetrically, it cannot tighten either. The snapshot is simply what we read. */
    public void testRuntimeSyspropTighteningIsAlsoInvisible()
    {
        System.setProperty( PERMIT, "true" );
        SealedSystemPropertiesBooleanProperty p = permitFlag();
        assertTrue( value( p ) );

        System.setProperty( PERMIT, "false" );

        assertTrue( "Runtime System property changes are not consulted at all.", value( p ) );
    }

    public void testRuntimeSyspropRemovalIsInvisible()
    {
        System.setProperty( PERMIT, "false" );
        SealedSystemPropertiesBooleanProperty p = permitFlag();
        assertFalse( value( p ) );

        System.clearProperty( PERMIT );

        assertFalse( value( p ) );
    }

    /** An explicit seal beforehand fixes what the flag will ever see from System properties. */
    public void testAnEarlierExplicitSealFixesWhatIsSeen()
    {
        System.setProperty( PERMIT, "false" );
        SealedSystemProperties.seal();
        System.setProperty( PERMIT, "true" );

        assertFalse( value( permitFlag() ) );
    }

    // ==================== disagreement: the safe value wins ====================

    /**
     *  An operator pinning a flag safe from the command line must not be overridden by
     *  application configuration. This is the boolean form of the whitelist's conservative
     *  intersection: where sources differ, the narrower answer stands.
     */
    public void testASealedSafeSyspropCannotBeLoosenedByConfig()
    {
        System.setProperty( PERMIT, "false" );
        SealedSystemPropertiesBooleanProperty p = permitFlag();

        assertFalse( "Precondition: pinned safe.", value( p ) );
        assertFalse( "Application config must not reopen it.", value( p, pcfg( PERMIT, "true" ) ) );
    }

    /** but config may tighten what System properties left permissive. */
    public void testConfigMayTightenASealedPermissiveSysprop()
    {
        System.setProperty( PERMIT, "true" );
        SealedSystemPropertiesBooleanProperty p = permitFlag();

        assertTrue( "Precondition: permissive.", value( p ) );
        assertFalse( value( p, pcfg( PERMIT, "false" ) ) );
    }

    // ==================== defaults and notices ====================

    public void testUnconfiguredWithASafeDefaultIsSilent()
    {
        SealedSystemPropertiesBooleanProperty p =
            new SealedSystemPropertiesBooleanProperty( ENFORCE, true );   // default == strongest

        assertTrue( value( p ) );
        assertTrue( "A safe default needs no comment: " + logger.warnings(), logger.warnings().isEmpty() );
    }

    public void testUnconfiguredWithAnUnsafeDefaultSaysSoOnce()
    {
        SealedSystemPropertiesBooleanProperty p =
            new SealedSystemPropertiesBooleanProperty( ENFORCE, true, false );

        assertFalse( value( p ) );
        assertEquals( "Exactly one nag: " + logger.warnings(),
                      1, logger.warningsContaining( "less secure value" ).size() );

        logger.clear();
        assertFalse( value( p ) );
        assertTrue( "and it must not repeat while nothing has changed: " + logger.warnings(),
                    logger.warnings().isEmpty() );
    }

    /**
     *  Re-entering the unconfigured state re-arms the notice. An application that withdraws
     *  its configuration is running on a default again, and should be told so.
     */
    public void testWithdrawingConfigurationReArmsTheNotice()
    {
        SealedSystemPropertiesBooleanProperty p =
            new SealedSystemPropertiesBooleanProperty( ENFORCE, true, false );

        assertFalse( value( p ) );
        assertEquals( 1, logger.warningsContaining( "less secure value" ).size() );

        assertFalse( "explicitly configured now", value( p, pcfg( ENFORCE, "false" ) ) );
        logger.clear();

        assertFalse( "and unconfigured again", value( p ) );
        assertEquals( "The notice must fire again for a newly unconfigured flag: " + logger.warnings(),
                      1, logger.warningsContaining( "less secure value" ).size() );
    }

    // ==================== what Details reports ====================

    /**
     *  This implementation never consults live System properties, so it must not report them.
     *  A caller asking Details whether the flag was unconfigured would otherwise get an answer
     *  contradicting the decision that was actually made.
     */
    public void testDetailsReportsNoCurrentSystemPropertyEvenWhenOneIsSet()
    {
        AbstractBooleanProperty.Details[] holder = new AbstractBooleanProperty.Details[1];
        SealedSystemPropertiesBooleanProperty p = permitFlag();

        // seal first, *then* set the property, or it would simply be captured in the snapshot --
        // constructing the flag does not seal, and the first getValue would
        SealedSystemProperties.seal();
        System.setProperty( PERMIT, "false" );
        p.getValue( null, logger, holder );

        assertNull( "A source the decision ignored must not be reported as consulted.",
                    holder[0].getCurrentSystemPropertyIfConsulted() );
        assertTrue( "and the flag must read as unconfigured, agreeing with the decision.",
                    holder[0].isUnconfigured() );
    }
}
