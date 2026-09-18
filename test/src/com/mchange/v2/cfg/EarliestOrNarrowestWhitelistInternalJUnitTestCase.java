package com.mchange.v2.cfg;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import com.mchange.v2.cfg.PropertiesConfigUtils.WhitelistInfo;
import com.mchange.v2.cfg.PropertiesConfigUtils.EarliestOrNarrowestWhitelistManager;

/**
 *  EarliestOrNarrowestWhitelistManager: a whitelist that can tighten after startup but never
 *  loosen.
 *
 *  <p>System properties are writable by any code in the process and, with SecurityManager
 *  permanently disabled, unguarded. A whitelist read live therefore means only what it meant
 *  at the instant it was read. This manager fixes its answer at the first lookup and
 *  thereafter honors only changes that narrow it -- a ratchet, in the sense that seccomp
 *  filters and OpenBSD's pledge are ratchets, rather than the pure read-once latch the JDK
 *  uses for its serialization filter.</p>
 *
 *  <p>Several cases below pin <i>limitations</i> rather than features: additions to a latched
 *  key are ignored, whole new subkeys are invisible, an unconfigured first lookup latches
 *  deny-all, and a deny-all is irreversible. Those are the documented consequences of the
 *  design -- only narrowing is supported after first lookup -- and they are tested precisely
 *  because someone reading the class later should be able to tell a deliberate limitation
 *  from a bug.</p>
 *
 *  <p>The sysprops snapshot is a one-shot static shared by the whole JVM, so these reset it
 *  reflectively around each case; see SealedSystemPropertiesInternalJUnitTestCase for why
 *  that lives only in tests. Order matters within each case: properties must be set
 *  <i>before</i> the first lookup, which is what seals and latches.</p>
 */
public class EarliestOrNarrowestWhitelistInternalJUnitTestCase extends TestCase
{
    private final static String BASE = "com.mchange.v2.cfg.junit.eon";
    private final static String WL   = BASE + ".whitelist";
    private final static String OVER = BASE + ".overrideWhitelist";
    private final static String DEPR = "com.mchange.v2.cfg.junit.eonOldWhitelist";

    private final static String WILDCARD = "*";
    private final static String DENY_ALL = "[]";

    private final static String ALPHA = "com.example.Alpha";
    private final static String BETA  = "com.example.Beta";
    private final static String GAMMA = "com.example.Gamma";

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
            if ( ours( k ) )
                System.setProperty( k, saved.getProperty( k ) );
        unseal();
    }

    private static void unseal() throws Exception
    {
        Field f = SealedSystemProperties.class.getDeclaredField( "theProperties" );
        f.setAccessible( true );
        f.set( null, null );
    }

    private static boolean ours( String k )
    { return k.startsWith( BASE ) || k.startsWith( DEPR ); }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( ours( k ) )
                doomed.add( k );
        for ( String k : doomed )
            System.clearProperty( k );
    }

    private EarliestOrNarrowestWhitelistManager manager()
    { return new EarliestOrNarrowestWhitelistManager( BASE, null ); }

    private EarliestOrNarrowestWhitelistManager migratingManager()
    { return new EarliestOrNarrowestWhitelistManager( BASE, DEPR ); }

    private Set<String> lookup( EarliestOrNarrowestWhitelistManager wm )
    { return lookup( wm, null ); }

    private Set<String> lookup( EarliestOrNarrowestWhitelistManager wm, PropertiesConfig pcfg )
    { return wm.collectWhitelistInfoSyspropsPropertiesConfig( pcfg, logger ).getWhitelist(); }

    private static PropertiesConfig pcfg( String... keysAndValues )
    {
        Properties p = new Properties();
        for ( int i = 0; i < keysAndValues.length; i += 2 )
            p.setProperty( keysAndValues[i], keysAndValues[i + 1] );
        return new BasicMultiPropertiesConfig( "/notional-test-resource", p );
    }

    private static Set<String> setOf( String... elems )
    { return new HashSet<String>( Arrays.asList( elems ) ); }

    // ==================== the latch ====================

    /** Constructing a manager must not seal -- ReferenceableUtils builds its managers in a
     *  static initializer, and sealing there would drag the instant back to class loading,
     *  which is the fragility this design replaced. */
    public void testConstructionDoesNotSeal()
    {
        manager();

        assertFalse( "Only an actual lookup may seal.", SealedSystemProperties.isSealed() );
    }

    public void testFirstLookupSeals()
    {
        System.setProperty( WL + ".layerOne", ALPHA );

        lookup( manager() );

        assertTrue( SealedSystemProperties.isSealed() );
    }

    /** An explicit seal beforehand is honored: the manager sees the sealed snapshot. */
    public void testAnEarlierExplicitSealFixesWhatTheManagerSees()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        SealedSystemProperties.seal();
        System.setProperty( WL + ".layerOne", ALPHA + "," + BETA );

        assertEquals( "Properties set after an explicit seal are not in the snapshot.",
                      setOf( ALPHA ), lookup( manager() ) );
    }

    // ==================== widening is refused ====================

    public void testWideningViaSyspropsIsIgnored()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        assertEquals( setOf( ALPHA ), lookup( wm ) );

        System.setProperty( WL + ".layerOne", ALPHA + "," + BETA );

        assertEquals( "A later addition must not take effect.", setOf( ALPHA ), lookup( wm ) );
    }

    /** and the config side is latched too, not merely the System properties snapshot. */
    public void testWideningViaAReplacedPropertiesConfigIsIgnored()
    {
        EarliestOrNarrowestWhitelistManager wm = manager();
        assertEquals( setOf( ALPHA ), lookup( wm, pcfg( WL + ".layerOne", ALPHA ) ) );

        assertEquals( "A widened PropertiesConfig must not widen the whitelist.",
                      setOf( ALPHA ), lookup( wm, pcfg( WL + ".layerOne", ALPHA + "," + BETA ) ) );
    }

    /**
     *  A whole new subkey added after the latch is invisible. Re-resolution consults only the
     *  keys the whitelist was originally computed from, which is what stops a new subkey from
     *  being a back door into widening. A late-initializing layer that registers its own
     *  entries gets nothing -- a limitation, deliberately taken.
     */
    public void testAnEntirelyNewSubkeyAddedLaterIsIgnored()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        lookup( wm );

        System.setProperty( WL + ".lateLayer", BETA );

        assertEquals( setOf( ALPHA ), lookup( wm ) );
    }

    // ==================== narrowing is honored ====================

    public void testNarrowingViaSyspropsIsHonored()
    {
        System.setProperty( WL + ".layerOne", ALPHA + "," + BETA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        assertEquals( setOf( ALPHA, BETA ), lookup( wm ) );

        System.setProperty( WL + ".layerOne", ALPHA );

        assertEquals( setOf( ALPHA ), lookup( wm ) );
    }

    public void testNarrowingViaPropertiesConfigIsHonored()
    {
        EarliestOrNarrowestWhitelistManager wm = manager();
        assertEquals( setOf( ALPHA, BETA ), lookup( wm, pcfg( WL + ".layerOne", ALPHA + "," + BETA ) ) );

        assertEquals( setOf( ALPHA ), lookup( wm, pcfg( WL + ".layerOne", ALPHA ) ) );
    }

    /** Removing a key entirely is a narrowing, and must not be mistaken for an error. */
    public void testRemovingALatchedKeyNarrowsRatherThanFailing()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        lookup( wm );

        System.clearProperty( WL + ".layerOne" );

        assertTrue( "A key that is gone contributes nothing.", lookup( wm ).isEmpty() );
    }

    // ==================== the wildcard, in both directions ====================

    /**
     *  Set intersection alone gets this wrong in both directions: {*} against {Alpha} is
     *  empty, and {Alpha} against {*} is empty, so a lone wildcard would turn the most
     *  permissive configuration into the most restrictive one.
     */
    public void testEarliestWildcardNarrowsToAConcreteList()
    {
        System.setProperty( WL + ".layerOne", WILDCARD );
        EarliestOrNarrowestWhitelistManager wm = manager();
        assertEquals( setOf( WILDCARD ), lookup( wm ) );

        System.setProperty( WL + ".layerOne", ALPHA );

        assertEquals( "Narrowing away from accept-all must yield the concrete list.",
                      setOf( ALPHA ), lookup( wm ) );
    }

    public void testLaterWideningToAWildcardIsIgnored()
    {
        System.setProperty( WL + ".layerOne", ALPHA + "," + BETA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        lookup( wm );

        System.setProperty( WL + ".layerOne", WILDCARD );

        assertEquals( "'*' is the widest possible change, and must not take effect.",
                      setOf( ALPHA, BETA ), lookup( wm ) );
    }

    /**
     *  The escalation the base class's removal of a non-unique '*' exists to prevent: were it
     *  retained, narrowing {*,Alpha} against {*,Beta} would leave exactly {*}.
     */
    public void testNarrowingCannotManufactureAWildcard()
    {
        System.setProperty( WL + ".layerOne", WILDCARD + "," + ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        assertEquals( setOf( ALPHA ), lookup( wm ) );

        System.setProperty( WL + ".layerOne", WILDCARD + "," + BETA );
        Set<String> after = lookup( wm );

        assertFalse( "A wildcard must never be reachable by narrowing: " + after,
                     after.contains( WILDCARD ) );
        assertTrue( after.isEmpty() );
    }

    // ==================== deny-all ====================

    public void testDenyAllAppearingLaterIsHonored()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        lookup( wm );

        System.setProperty( WL + ".layerOne", DENY_ALL );

        assertTrue( lookup( wm ).isEmpty() );
    }

    /** including from a key family that did not define the whitelist. */
    public void testDenyAllInTheOverrideKeyVetoesALatchedWhitelist()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        lookup( wm );

        System.setProperty( OVER, DENY_ALL );

        assertTrue( lookup( wm ).isEmpty() );
    }

    public void testDenyAllInADeprecatedKeyVetoesALatchedWhitelist()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = migratingManager();
        lookup( wm );

        System.setProperty( DEPR, DENY_ALL );

        assertTrue( lookup( wm ).isEmpty() );
    }

    /**
     *  A deny-all cannot be undone. Under a ratchet privilege never increases, so removing
     *  the token does not restore service -- only a restart does. Deliberate, and the reason
     *  the transition is warned about loudly.
     */
    public void testDenyAllIsIrreversible()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        lookup( wm );

        System.setProperty( OVER, DENY_ALL );
        assertTrue( lookup( wm ).isEmpty() );

        System.clearProperty( OVER );

        assertTrue( "Removing the token must not restore the whitelist.", lookup( wm ).isEmpty() );
    }

    /**
     *  and an unconfigured first lookup latches deny-all for good. This is the sharpest edge
     *  of an implicit seal: config that arrives afterward is ignored, which is why a
     *  deployment that controls its own startup should call seal() at a point it chooses.
     */
    public void testAnUnconfiguredFirstLookupLatchesDenyAll()
    {
        EarliestOrNarrowestWhitelistManager wm = manager();
        assertTrue( lookup( wm ).isEmpty() );

        System.setProperty( WL + ".layerOne", ALPHA );

        assertTrue( "Configuration arriving after the first lookup is ignored.",
                    lookup( wm ).isEmpty() );
    }

    // ==================== warnings ====================

    /** The supported arrangement -- set config, then leave it alone -- must be silent. */
    public void testRepeatedLookupsWithoutChangeAreSilent()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();

        lookup( wm ); lookup( wm ); lookup( wm );

        assertTrue( "The 99% path must not log: " + logger.warnings(), logger.warnings().isEmpty() );
    }

    public void testAHonoredNarrowingIsReportedOnceWithTheRule()
    {
        System.setProperty( WL + ".layerOne", ALPHA + "," + BETA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        lookup( wm );
        logger.clear();

        System.setProperty( WL + ".layerOne", ALPHA );
        lookup( wm );

        assertEquals( "Exactly one warning for one narrowing: " + logger.warnings(),
                      1, logger.warningsContaining( "narrowing" ).size() );
        assertTrue( "and it must state the rule, since widening is otherwise silent.",
                    logger.sawWarningContaining( "narrowing", "silently ignored" ) );

        logger.clear();
        lookup( wm );
        assertTrue( "A steady state must not keep warning: " + logger.warnings(),
                    logger.warnings().isEmpty() );
    }

    /** A widening is ignored silently -- a documented limitation, pinned so it stays deliberate. */
    public void testAnIgnoredWideningIsSilent()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        lookup( wm );
        logger.clear();

        System.setProperty( WL + ".layerOne", ALPHA + "," + GAMMA );
        lookup( wm );

        assertTrue( "Widening changes are ignored without comment: " + logger.warnings(),
                    logger.warnings().isEmpty() );
    }

    public void testADenyAllVetoNamesTheKeyResponsible()
    {
        System.setProperty( WL + ".layerOne", ALPHA );
        EarliestOrNarrowestWhitelistManager wm = manager();
        lookup( wm );
        logger.clear();

        System.setProperty( OVER, DENY_ALL );
        lookup( wm );

        assertTrue( "The operator must be told which key disabled the whitelist: " + logger.warnings(),
                    logger.sawWarningContaining( DENY_ALL, OVER ) );
    }
}
