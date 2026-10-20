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

/**
 *  WhitelistManager, which factors out the shape every whitelist in
 *  this library is meant to share: a base key whose <code>.whitelist</code> subkeys union
 *  additively so independent layers can each contribute, an <code>.overrideWhitelist</code>
 *  that replaces that union outright, and an optional deprecated single key that keeps a
 *  pre-existing deployment working while it migrates by narrowing whatever the other keys
 *  resolved to.
 *
 *  <p>Declared in-package because BasicMultiPropertiesConfig is not public, and the
 *  conservative intersection between System properties and supplied configuration -- the
 *  security-relevant half of this class -- cannot be exercised without a PropertiesConfig
 *  to disagree with.</p>
 *
 *  <p>ByNameInstantiationUtilsJUnitTestCase covers the same machinery as its one current
 *  client sees it. This covers what that client cannot reach: it constructs its manager
 *  with <code>null</code> for the deprecated key, so the deprecated path and the narrowing
 *  it performs are tested only here.</p>
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

    //  The deprecated key NARROWS; it does not replace. The alternative would be for it to
    //  win outright, on the reasoning that honoring the new keys alongside it could widen a
    //  whitelist an existing deployment had already narrowed. Intersecting meets that
    //  reasoning without a hazard winning outright would introduce: because selecting WHICH
    //  key governs sits outside
    //  the per-key intersection between System properties and supplied config, a deprecated
    //  key injected into lower-trust config could displace an override an operator had pinned
    //  in System properties -- and displace it with something WIDER. Intersection makes the
    //  deprecated key incapable of widening anything, whoever wrote it and wherever from.

    /**
     *  A deployment still carrying only the deprecated key keeps exactly the behavior it had.
     *  This is the compatibility the migration promises, and the case the reorder must not
     *  disturb.
     */
    public void testDeprecatedKeyAloneStillDecides()
    {
        System.setProperty( DEPRECATED, ALPHA + "," + BETA );

        WhitelistInfo info = collect( migrating() );

        assertEquals( WhitelistInfo.Source.DEPRECATED, info.getSource() );
        assertEquals( setOf( ALPHA, BETA ), info.getWhitelist() );
    }

    /**
     *  With the new keys also present, the result is the INTERSECTION rather than the
     *  deprecated key alone. Entries only the deprecated key names are dropped, which is what
     *  makes it unable to widen.
     */
    public void testDeprecatedKeyIntersectsWithTheUnionRatherThanReplacingIt()
    {
        System.setProperty( DEPRECATED, ALPHA + "," + BETA );
        System.setProperty( WHITELIST + ".layerOne", BETA + "," + GAMMA );

        WhitelistInfo info = collect( migrating() );

        assertEquals( setOf( BETA ), info.getWhitelist() );
    }

    /** ...and with an override, likewise. */
    public void testDeprecatedKeyIntersectsWithAnOverrideRatherThanReplacingIt()
    {
        System.setProperty( DEPRECATED, ALPHA + "," + BETA );
        System.setProperty( OVERRIDE, BETA + "," + GAMMA );

        WhitelistInfo info = collect( migrating() );

        assertEquals( setOf( BETA ), info.getWhitelist() );
    }

    /**
     *  The property the change exists for, stated as the attack it prevents. An operator pins
     *  an override in System properties; the deprecated key arrives from supplied config,
     *  naming something the override excluded. It must not get in.
     */
    public void testADeprecatedKeyInSuppliedConfigCannotWidenAnOverrideInSystemProperties()
    {
        System.setProperty( OVERRIDE, ALPHA );

        WhitelistInfo info = collect( migrating(), pcfg( DEPRECATED, GAMMA ) );

        assertFalse( "supplied config must not be able to add an entry the override excluded",
                     info.getWhitelist().contains( GAMMA ) );
        assertTrue( "and failing closed is the right direction when they are disjoint",
                    info.getWhitelist().isEmpty() );
    }

    /** Narrowing from supplied config is still allowed -- only widening is refused. */
    public void testADeprecatedKeyInSuppliedConfigMayNarrowAnOverrideInSystemProperties()
    {
        System.setProperty( OVERRIDE, ALPHA + "," + BETA );

        WhitelistInfo info = collect( migrating(), pcfg( DEPRECATED, ALPHA ) );

        assertEquals( setOf( ALPHA ), info.getWhitelist() );
    }

    /** Disjoint lists intersect to nothing, which is deny-all rather than either list. */
    public void testWhollyDisjointDeprecatedAndNewKeysWhitelistNothing()
    {
        System.setProperty( DEPRECATED, ALPHA );
        System.setProperty( WHITELIST + ".layerOne", BETA );
        System.setProperty( OVERRIDE, GAMMA );

        WhitelistInfo info = collect( migrating() );

        assertEquals( WhitelistInfo.Source.DEPRECATED, info.getSource() );
        assertTrue( "no entry is named by both, so nothing is whitelisted", info.getWhitelist().isEmpty() );
    }

    /**
     *  An explicitly empty deprecated key still denies all, now by intersecting to nothing
     *  rather than by winning outright. Present-but-empty is a deny-all a deployment chose,
     *  and intersection preserves it for the same reason precedence used to.
     */
    public void testEmptyDeprecatedKeyStillDeniesAll()
    {
        System.setProperty( DEPRECATED, "" );
        System.setProperty( WHITELIST + ".layerOne", BETA );

        WhitelistInfo info = collect( migrating() );

        assertEquals( WhitelistInfo.Source.DEPRECATED, info.getSource() );
        assertTrue( info.getWhitelist().isEmpty() );
    }

    /**
     *  Both keys are reported as having contributed, since both did. A diagnostic that named
     *  only one would send a reader to the wrong half of their configuration.
     */
    public void testBothKeysAreReportedWhenBothContribute()
    {
        System.setProperty( DEPRECATED, ALPHA + "," + BETA );
        System.setProperty( OVERRIDE, BETA );

        WhitelistInfo info = collect( migrating() );

        assertTrue( "the deprecated key should be named: " + info.getFromKeys(),
                    info.getFromKeys().contains( DEPRECATED ) );
        assertTrue( "and so should the override: " + info.getFromKeys(),
                    info.getFromKeys().contains( OVERRIDE ) );
    }

    /** Absent, it yields to the new keys rather than narrowing them to nothing. */
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

    /**
     *  A wildcard survives only where the deprecated key is the whole story. Intersecting
     *  "{*, x}" with "{*, y}" yields "{*}" -- accept-any, WIDER than either side meant -- so
     *  that case denies all instead. Subtle enough to be refactored away by accident.
     */
    public void testAWildcardProducedOnlyByIntersectionIsRefused()
    {
        System.setProperty( DEPRECATED, "*," + ALPHA );
        System.setProperty( OVERRIDE,   "*," + BETA );

        WhitelistInfo info = collect( migrating() );

        assertFalse( "an accept-any whitelist must not be synthesized by intersection: " + info.getWhitelist(),
                     info.getWhitelist().contains( "*" ) );
        assertTrue( info.getWhitelist().isEmpty() );
    }

    /** ...while a deprecated-only wildcard is honored as it always was. */
    public void testADeprecatedOnlyWildcardIsHonored()
    {
        System.setProperty( DEPRECATED, "*" );

        WhitelistInfo info = collect( migrating() );

        assertEquals( setOf( "*" ), info.getWhitelist() );
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
