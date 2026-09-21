package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

/**
 *  SealedSystemPropertiesWhitelistManager: System properties as of the seal, application
 *  configuration honored per call.
 *
 *  <p>The counterpart of SealedSystemPropertiesBooleanProperty, and deliberately simpler than
 *  the ratchet it replaces. It holds no state, which is most of the point: two lookups with
 *  the same configuration give the same answer whatever happened in between, an early lookup
 *  cannot latch a whitelist a deployment has not finished configuring, and there is nothing
 *  for a test fixture to reset beyond the snapshot itself.</p>
 *
 *  <p>It still closes the channel the ratchet was built for. Runtime mutation of System
 *  properties cannot widen a whitelist, because runtime System properties are not read at
 *  all. What it gives up is runtime <i>narrowing</i> through System properties, which
 *  application configuration can do instead.</p>
 */
public class SealedSystemPropertiesWhitelistManagerInternalJUnitTestCase extends TestCase
{
    private final static String BASE = "com.mchange.v2.cfg.junit.sswl";
    private final static String WL   = BASE + ".whitelist";
    private final static String OVER = BASE + ".overrideWhitelist";

    private final static String ALPHA = "com.example.Alpha";
    private final static String BETA  = "com.example.Beta";

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
            if ( k.startsWith( BASE ) )
                System.setProperty( k, saved.getProperty( k ) );
        SecurityRatchetTestSupport.unsealSystemProperties();
    }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( k.startsWith( BASE ) )
                doomed.add( k );
        for ( String k : doomed )
            System.clearProperty( k );
    }

    private static PropertiesConfig pcfg( String... keysAndValues )
    {
        Properties p = new Properties();
        for ( int i = 0; i < keysAndValues.length; i += 2 )
            p.setProperty( keysAndValues[i], keysAndValues[i + 1] );
        return new BasicMultiPropertiesConfig( "/notional-test-resource", p );
    }

    private static Set<String> setOf( String... elems )
    { return new HashSet<String>( Arrays.asList( elems ) ); }

    private SealedSystemPropertiesWhitelistManager manager()
    { return new SealedSystemPropertiesWhitelistManager( BASE, null ); }

    private Set<String> lookup( SealedSystemPropertiesWhitelistManager wm )
    { return lookup( wm, null ); }

    private Set<String> lookup( SealedSystemPropertiesWhitelistManager wm, PropertiesConfig cfg )
    { return wm.collectWhitelistInfoSyspropsPropertiesConfig( cfg, logger ).getWhitelist(); }

    // ==================== System properties: sealed ====================

    public void testSealedSyspropWhitelistIsHonored()
    {
        System.setProperty( WL + ".layerOne", ALPHA );

        assertEquals( setOf( ALPHA ), lookup( manager() ) );
    }

    /** The threat the seal exists for: runtime property mutation must not widen a whitelist. */
    public void testRuntimeSyspropWideningIsInvisible()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        SealedSystemPropertiesWhitelistManager wm = manager();
        assertEquals( setOf( ALPHA ), lookup( wm ) );

        System.setProperty( WL + ".layerOne", ALPHA + ",com.example.EVIL" );

        assertEquals( "A later System property must not widen the whitelist.",
                      setOf( ALPHA ), lookup( wm ) );
    }

    /** A whole new subkey added at runtime is equally invisible. */
    public void testARuntimeSyspropSubkeyIsInvisible()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        SealedSystemPropertiesWhitelistManager wm = manager();
        lookup( wm );

        System.setProperty( WL + ".lateLayer", BETA );

        assertEquals( setOf( ALPHA ), lookup( wm ) );
    }

    /**
     *  and narrowing is invisible too. This is what the design trades away: the snapshot is
     *  simply what is read, in both directions, rather than a floor that later changes may
     *  lower.
     */
    public void testRuntimeSyspropNarrowingIsAlsoInvisible()
    {
        System.setProperty( WL + ".layerOne", ALPHA + "," + BETA );
        SealedSystemPropertiesWhitelistManager wm = manager();
        assertEquals( setOf( ALPHA, BETA ), lookup( wm ) );

        System.setProperty( WL + ".layerOne", ALPHA );

        assertEquals( "Runtime System properties are not consulted at all.",
                      setOf( ALPHA, BETA ), lookup( wm ) );
    }

    public void testRuntimeSyspropRemovalIsInvisible()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        SealedSystemPropertiesWhitelistManager wm = manager();
        lookup( wm );

        System.clearProperty( WL + ".layerOne" );

        assertEquals( setOf( ALPHA ), lookup( wm ) );
    }

    // ==================== application config: honored per call ====================

    public void testConfigIsHonoredAndMayWidenOrNarrowFreely()
    {
        SealedSystemPropertiesWhitelistManager wm = manager();

        assertEquals( setOf( ALPHA ), lookup( wm, pcfg( WL + ".layerOne", ALPHA ) ) );
        assertEquals( "Configuration may widen; nothing ratchets here.",
                      setOf( ALPHA, BETA ), lookup( wm, pcfg( WL + ".layerOne", ALPHA + "," + BETA ) ) );
        assertEquals( "and narrow again.",
                      setOf( ALPHA ), lookup( wm, pcfg( WL + ".layerOne", ALPHA ) ) );
    }

    /** A pcfg is an argument: withdrawing it withdraws its contribution. */
    public void testConfigInfluenceDoesNotOutliveTheCall()
    {
        SealedSystemPropertiesWhitelistManager wm = manager();

        assertEquals( setOf( ALPHA ), lookup( wm, pcfg( WL + ".layerOne", ALPHA ) ) );
        assertEquals( WhitelistInfo.Source.MISSING,
                      wm.collectWhitelistInfoSyspropsPropertiesConfig( null, logger ).getSource() );
    }

    /**
     *  Statelessness, stated directly: the answer is a function of the snapshot and the
     *  configuration supplied, and of nothing that happened earlier.
     */
    public void testTheSameConfigurationAlwaysGivesTheSameAnswer()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        SealedSystemPropertiesWhitelistManager wm = manager();

        PropertiesConfig one = pcfg( WL + ".layerTwo", BETA );
        Set<String> first = lookup( wm, one );

        lookup( wm, null );                                              // an intervening lookup
        lookup( wm, pcfg( WL + ".layerTwo", "com.example.Gamma" ) );     // and a different one

        assertEquals( "History must not affect the answer.", first, lookup( wm, one ) );
    }

    /**
     *  An unconfigured whitelist does not latch. Under the ratchet this was the sharpest
     *  edge -- one early lookup could pin deny-all for the life of the JVM, and configuration
     *  arriving afterward was ignored.
     */
    public void testAnUnconfiguredLookupDoesNotLatchDenyAll()
    {
        SealedSystemPropertiesWhitelistManager wm = manager();

        assertEquals( WhitelistInfo.Source.MISSING,
                      wm.collectWhitelistInfoSyspropsPropertiesConfig( null, logger ).getSource() );

        assertEquals( "Configuration arriving later must still be honored.",
                      setOf( ALPHA ), lookup( wm, pcfg( WL + ".layerOne", ALPHA ) ) );
    }

    // ==================== the inherited rules still apply ====================

    public void testSubkeysUnionAcrossSourcesAsBefore()
    {
        System.setProperty( WL + ".fromSysprops", ALPHA );

        assertEquals( setOf( ALPHA, BETA ), lookup( manager(), pcfg( WL + ".fromConfig", BETA ) ) );
    }

    public void testDisagreeingSourcesStillIntersect()
    {
        System.setProperty( WL + ".layerOne", ALPHA + "," + BETA );

        assertEquals( "Only what both sources name may be whitelisted.",
                      setOf( BETA ), lookup( manager(), pcfg( WL + ".layerOne", BETA + ",com.example.Gamma" ) ) );
    }

    public void testTheDenyAllSentinelStillDenies()
    {
        System.setProperty( WL + ".layerOne", ALPHA );

        assertTrue( lookup( manager(), pcfg( OVER, "[]" ) ).isEmpty() );
    }

    public void testTheWildcardStillPermitsEverything()
    {
        System.setProperty( WL + ".layerOne", "*" );

        assertEquals( setOf( "*" ), lookup( manager() ) );
    }

    public void testAnOverrideStillReplacesTheSubkeys()
    {
        System.setProperty( WL + ".layerOne", ALPHA );

        WhitelistInfo info = manager().collectWhitelistInfoSyspropsPropertiesConfig( pcfg( OVER, BETA ), logger );

        assertEquals( WhitelistInfo.Source.OVERRIDE, info.getSource() );
        assertEquals( setOf( BETA ), info.getWhitelist() );
    }
}
