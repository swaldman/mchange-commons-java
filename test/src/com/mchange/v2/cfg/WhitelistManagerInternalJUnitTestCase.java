package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import com.mchange.v2.log.MLevel;
import com.mchange.v2.log.MLog;
import com.mchange.v2.log.MLogger;

import com.mchange.v2.cfg.PropertiesConfigUtils.WhitelistInfo;
import com.mchange.v2.cfg.PropertiesConfigUtils.WhitelistManager;

/**
 *  PropertiesConfigUtils.WhitelistManager, which factors out the shape every whitelist in
 *  this library is meant to share: a base key whose <code>.whitelist</code> subkeys union
 *  additively so independent layers can each contribute, an <code>.overrideWhitelist</code>
 *  that replaces that union outright, and an optional deprecated single key that keeps a
 *  pre-existing deployment working while it migrates.
 *
 *  <p>Declared in-package because BasicMultiPropertiesConfig is not public, and the
 *  conservative intersection between System properties and supplied configuration -- the
 *  security-relevant half of this class -- cannot be exercised without a PropertiesConfig
 *  to disagree with.</p>
 *
 *  <p>ByNameInstantiationUtilsJUnitTestCase covers the same machinery as its one current
 *  client sees it. This covers what that client cannot reach: it constructs its manager
 *  with <code>null</code> for the deprecated key, so the deprecated path and the precedence
 *  it claims are tested only here.</p>
 */
public class WhitelistManagerInternalJUnitTestCase extends TestCase
{
    private final static String BASE       = "com.mchange.v2.cfg.junit.testWhitelist";
    private final static String DEPRECATED = "com.mchange.v2.cfg.junit.oldTestWhitelist";

    private final static String WHITELIST = BASE + ".whitelist";
    private final static String OVERRIDE  = BASE + ".overrideWhitelist";

    private final static String ALPHA = "com.example.Alpha";
    private final static String BETA  = "com.example.Beta";
    private final static String GAMMA = "com.example.Gamma";

    private MLogger logger;
    private Properties saved;

    @Override
    public void setUp()
    {
        logger = MLog.getLogger( WhitelistManagerInternalJUnitTestCase.class );
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
    }

    @Override
    public void tearDown()
    {
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( ours( k ) )
                System.setProperty( k, saved.getProperty( k ) );
    }

    private static boolean ours( String k )
    { return k.startsWith( BASE ) || k.startsWith( DEPRECATED ); }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( ours( k ) )
                doomed.add( k );
        for ( String k : doomed )
            System.clearProperty( k );
    }

    /** a manager that honors the deprecated key */
    private WhitelistManager migrating()
    { return new WhitelistManager( BASE, DEPRECATED ); }

    /** a manager with no deprecated key at all, as ByNameInstantiationUtils constructs one */
    private WhitelistManager fresh()
    { return new WhitelistManager( BASE, null ); }

    private WhitelistInfo collect( WhitelistManager wm )
    { return wm.collectWhitelistInfoSyspropsPropertiesConfig( null, logger ); }

    private WhitelistInfo collect( WhitelistManager wm, PropertiesConfig pcfg )
    { return wm.collectWhitelistInfoSyspropsPropertiesConfig( pcfg, logger ); }

    private static PropertiesConfig pcfg( String... keysAndValues )
    {
        Properties props = new Properties();
        for ( int i = 0; i < keysAndValues.length; i += 2 )
            props.setProperty( keysAndValues[i], keysAndValues[i + 1] );
        return new BasicMultiPropertiesConfig( "/notional-test-resource", props );
    }

    private static Set<String> setOf( String... elems )
    { return new HashSet<String>( Arrays.asList( elems ) ); }

    // ---------- key derivation ----------

    /**
     *  The subkeys are derived from the base, which is what lets a caller name one key and
     *  get the whole family. A rename of the base must move all of them together.
     */
    public void testKeysDeriveFromTheBaseKey()
    {
        WhitelistManager wm = migrating();

        assertEquals( BASE, wm.getTopLevelBaseKey() );
        assertEquals( BASE + ".whitelist", wm.getWhitelistBaseKey() );
        assertEquals( BASE + ".overrideWhitelist", wm.getOverrideWhitelistKey() );
        assertEquals( DEPRECATED, wm.getDeprecatedKey() );
    }

    public void testDeprecatedKeyIsNullWhenThereIsNone()
    { assertNull( fresh().getDeprecatedKey() ); }

    // ---------- the main whitelist ----------

    public void testNothingConfiguredIsMissing()
    {
        WhitelistInfo info = collect( fresh() );

        assertEquals( WhitelistInfo.Source.MISSING, info.getSource() );
        assertTrue( info.getWhitelist().isEmpty() );
    }

    public void testSubkeysUnion()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA );
        System.setProperty( WHITELIST + ".layerTwo", BETA + "," + GAMMA );

        WhitelistInfo info = collect( fresh() );

        assertEquals( WhitelistInfo.Source.MAIN_WHITELIST, info.getSource() );
        assertEquals( setOf( ALPHA, BETA, GAMMA ), info.getWhitelist() );
    }

    /** The naked prefix is a whitelist key in its own right, not merely a namespace. */
    public void testBareWhitelistKeyParticipatesInTheUnion()
    {
        System.setProperty( WHITELIST, ALPHA );
        System.setProperty( WHITELIST + ".layerOne", BETA );

        assertEquals( setOf( ALPHA, BETA ), collect( fresh() ).getWhitelist() );
    }

    /** An explicitly empty value is a configured deny-all, distinguishable from MISSING. */
    public void testExplicitlyEmptyIsConfiguredNotMissing()
    {
        System.setProperty( WHITELIST + ".layerOne", "" );

        WhitelistInfo info = collect( fresh() );

        assertEquals( WhitelistInfo.Source.MAIN_WHITELIST, info.getSource() );
        assertTrue( info.getWhitelist().isEmpty() );
    }

    // ---------- override ----------

    public void testOverrideReplacesTheUnionRatherThanAddingToIt()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA );
        System.setProperty( OVERRIDE, BETA );

        WhitelistInfo info = collect( fresh() );

        assertEquals( WhitelistInfo.Source.OVERRIDE, info.getSource() );
        assertEquals( "The overridden union must be gone, not merged.", setOf( BETA ), info.getWhitelist() );
    }

    /** An empty override is still an override: deny-all, deliberately chosen. */
    public void testEmptyOverrideStillOverrides()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA );
        System.setProperty( OVERRIDE, "" );

        WhitelistInfo info = collect( fresh() );

        assertEquals( WhitelistInfo.Source.OVERRIDE, info.getSource() );
        assertTrue( info.getWhitelist().isEmpty() );
    }

    // ---------- the deprecated key ----------

    /**
     *  A deployment still carrying the deprecated key keeps the behavior it had. Honoring
     *  the new keys alongside it would silently widen a whitelist that a deployment had
     *  narrowed, so the deprecated key wins outright and the class nags about it instead.
     */
    public void testDeprecatedKeyTakesPrecedenceOverEverything()
    {
        System.setProperty( DEPRECATED, ALPHA );
        System.setProperty( WHITELIST + ".layerOne", BETA );
        System.setProperty( OVERRIDE, GAMMA );

        WhitelistInfo info = collect( migrating() );

        assertEquals( WhitelistInfo.Source.DEPRECATED, info.getSource() );
        assertEquals( setOf( ALPHA ), info.getWhitelist() );
    }

    /**
     *  and an explicitly empty deprecated key wins too. Present-but-empty is a deny-all a
     *  deployment chose; falling through to the new keys would quietly widen it.
     */
    public void testEmptyDeprecatedKeyStillTakesPrecedence()
    {
        System.setProperty( DEPRECATED, "" );
        System.setProperty( WHITELIST + ".layerOne", BETA );

        WhitelistInfo info = collect( migrating() );

        assertEquals( WhitelistInfo.Source.DEPRECATED, info.getSource() );
        assertTrue( info.getWhitelist().isEmpty() );
    }

    /** Absent, it yields to the new keys rather than shadowing them. */
    public void testAbsentDeprecatedKeyYieldsToTheNewKeys()
    {
        System.setProperty( WHITELIST + ".layerOne", BETA );

        WhitelistInfo info = collect( migrating() );

        assertEquals( WhitelistInfo.Source.MAIN_WHITELIST, info.getSource() );
        assertEquals( setOf( BETA ), info.getWhitelist() );
    }

    /** A manager declaring no deprecated key must not read one, however it is set. */
    public void testManagerWithoutADeprecatedKeyIgnoresIt()
    {
        System.setProperty( DEPRECATED, ALPHA );
        System.setProperty( WHITELIST + ".layerOne", BETA );

        WhitelistInfo info = collect( fresh() );

        assertEquals( WhitelistInfo.Source.MAIN_WHITELIST, info.getSource() );
        assertEquals( setOf( BETA ), info.getWhitelist() );
    }

    // ---------- System properties vs supplied configuration ----------

    /**
     *  Where both sources set a key, the whitelist is their *intersection*. A whitelist is
     *  a security control, so a name only one source vouches for is not vouched for; this
     *  is what stops a writable config file from widening a whitelist pinned on the command
     *  line, and vice versa.
     */
    public void testDisagreeingSourcesIntersect()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA + "," + BETA );

        WhitelistInfo info = collect( fresh(), pcfg( WHITELIST + ".layerOne", BETA + "," + GAMMA ) );

        assertEquals( "Only the name both sources name may be whitelisted.",
                      setOf( BETA ), info.getWhitelist() );
    }

    /** Disjoint sources whitelist nothing at all -- but that is still a configured whitelist. */
    public void testWhollyDisagreeingSourcesWhitelistNothing()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA );

        WhitelistInfo info = collect( fresh(), pcfg( WHITELIST + ".layerOne", GAMMA ) );

        assertEquals( WhitelistInfo.Source.MAIN_WHITELIST, info.getSource() );
        assertTrue( info.getWhitelist().isEmpty() );
    }

    /**
     *  Intersection is per key, before the union across keys. So a key set in only one
     *  source contributes fully; it is not silently cancelled by the other source's silence.
     */
    public void testKeysSetInOnlyOneSourceContributeFully()
    {
        System.setProperty( WHITELIST + ".fromSysprops", ALPHA );

        WhitelistInfo info = collect( fresh(), pcfg( WHITELIST + ".fromConfig", BETA ) );

        assertEquals( setOf( ALPHA, BETA ), info.getWhitelist() );
    }

    public void testConfigOnlyWhitelistIsHonored()
    {
        WhitelistInfo info = collect( fresh(), pcfg( WHITELIST + ".layerOne", ALPHA ) );

        assertEquals( WhitelistInfo.Source.MAIN_WHITELIST, info.getSource() );
        assertEquals( setOf( ALPHA ), info.getWhitelist() );
    }

    // ---------- descriptors ----------

    /**
     *  Diagnostics name the key that actually decided the whitelist. Naming the subkeys
     *  while an override or the deprecated key was in force would send a reader to edit a
     *  key whose contents had been discarded.
     */
    public void testDescriptorNamesTheEffectiveKey()
    {
        WhitelistManager wm = migrating();

        String main = wm.makeWhitelistDescriptor( WhitelistInfo.Source.MAIN_WHITELIST );
        assertTrue( main, main.contains( WHITELIST ) );
        assertFalse( main, main.contains( OVERRIDE ) );

        String override = wm.makeWhitelistDescriptor( WhitelistInfo.Source.OVERRIDE );
        assertTrue( override, override.contains( OVERRIDE ) );

        String deprecated = wm.makeWhitelistDescriptor( WhitelistInfo.Source.DEPRECATED );
        assertTrue( deprecated, deprecated.contains( DEPRECATED ) );
    }

    /**
     *  The MISSING descriptor is the one a reader most needs a key from -- it accompanies a
     *  refusal for a whitelist nobody has configured -- so it must still say where to put one.
     */
    public void testMissingDescriptorStillNamesWhereToConfigureOne()
    {
        String missing = migrating().makeWhitelistDescriptor( WhitelistInfo.Source.MISSING );

        assertTrue( missing, missing.contains( WHITELIST ) );
    }
}
