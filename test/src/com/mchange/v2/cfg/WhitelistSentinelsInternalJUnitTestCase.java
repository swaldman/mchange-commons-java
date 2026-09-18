package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import com.mchange.v2.cfg.PropertiesConfigUtils.WhitelistInfo;
import com.mchange.v2.cfg.PropertiesConfigUtils.WhitelistManager;

/**
 *  The two sentinels a whitelist may contain, and the rules that make them safe.
 *
 *  <p><b>'[]' denies.</b> Layered whitelists compose additively, so a layer could widen but
 *  never narrow. '[]' is the missing veto: present in any key of any of the three key
 *  families, in either System properties or supplied configuration, it empties the whitelist
 *  outright. It must survive the usual intersection rule, because intersection is
 *  conservative for <i>permissions</i> -- dropping one narrows -- while '[]' is a
 *  <i>denial</i>, and dropping a denial widens.</p>
 *
 *  <p><b>'*' permits everything, but only when it is genuinely unique.</b> A '*' sharing a
 *  whitelist with real entries is not a wildcard, or one layer's '*' would silently open a
 *  gate another layer narrowed. Subtler: {A,*} from System properties intersected with
 *  {B,*} from configuration yields exactly {*}, so two sources that each meant to narrow
 *  would combine into accept-everything. An apparently unique '*' is therefore re-checked
 *  against the raw values that produced it.</p>
 *
 *  <p>Both rules resolve conflicts toward denial, and both are exercised here with
 *  warnings disabled as well as enabled: these are security decisions, and must not depend
 *  on log level.</p>
 */
public class WhitelistSentinelsInternalJUnitTestCase extends TestCase
{
    private final static String BASE       = "com.mchange.v2.cfg.junit.sentinelWhitelist";
    private final static String DEPRECATED = "com.mchange.v2.cfg.junit.oldSentinelWhitelist";

    private final static String WHITELIST = BASE + ".whitelist";
    private final static String OVERRIDE  = BASE + ".overrideWhitelist";

    private final static String DENY_ALL = "[]";
    private final static String WILDCARD = "*";

    private final static String ALPHA = "com.example.Alpha";
    private final static String BETA  = "com.example.Beta";

    private CapturingMLogger logger;
    private Properties saved;

    @Override
    public void setUp()
    {
        logger = new CapturingMLogger();
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

    private WhitelistManager fresh()
    { return new WhitelistManager( BASE, null ); }

    private WhitelistManager migrating()
    { return new WhitelistManager( BASE, DEPRECATED ); }

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

    private void assertDenied( String why, WhitelistInfo info )
    { assertTrue( why + " -- expected an empty, deny-all whitelist, got " + info, info.getWhitelist().isEmpty() ); }

    // ======================= '[]' denies, wherever it appears =======================

    /** The simple case: a whitelist whose only entry is the token. */
    public void testLoneDenyAllTokenEmptiesTheWhitelist()
    {
        System.setProperty( WHITELIST + ".layerOne", DENY_ALL );

        WhitelistInfo info = collect( fresh() );

        assertDenied( "A lone '[]'", info );
        assertEquals( "An explicit deny-all is configured, so it is not MISSING.",
                      WhitelistInfo.Source.MAIN_WHITELIST, info.getSource() );
    }

    /** Within one key, '[]' discards its companions rather than joining them. */
    public void testDenyAllTokenAmongEntriesInASingleKey()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA + "," + DENY_ALL );

        assertDenied( "'[]' alongside a real entry", collect( fresh() ) );
        assertTrue( "The operator should be told the other entries were dropped.",
                    logger.sawWarningContaining( DENY_ALL, "trumps everything" ) );
    }

    /**
     *  The layering case this sentinel exists for: one layer names classes, another vetoes.
     *  Without '[]' there was no way for the second layer to say so -- the union only grows.
     */
    public void testDenyAllTokenInOneSubkeyVetoesAnother()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA );
        System.setProperty( WHITELIST + ".layerTwo", DENY_ALL );

        assertDenied( "A vetoing layer", collect( fresh() ) );
    }

    /** Against the opposite sentinel, denial wins. */
    public void testDenyAllTokenBeatsTheWildcard()
    {
        System.setProperty( WHITELIST + ".layerOne", WILDCARD );
        System.setProperty( WHITELIST + ".layerTwo", DENY_ALL );

        assertDenied( "'[]' against '*'", collect( fresh() ) );
    }

    // ------- surviving the intersection rule, in both directions -------

    /**
     *  The hazard that motivated the raw-value check. Intersection is conservative for
     *  permissions but not for denials: {'[]'} intersected with {Alpha} is empty, so a key
     *  carrying the operator's veto would contribute nothing at all, and any OTHER key's
     *  entries would survive. The veto must outrank the intersection.
     */
    public void testDenyAllFromSyspropsSurvivesADisagreeingConfig()
    {
        System.setProperty( WHITELIST + ".layerOne", DENY_ALL );
        System.setProperty( WHITELIST + ".layerTwo", BETA );

        WhitelistInfo info = collect( fresh(), pcfg( WHITELIST + ".layerOne", ALPHA ) );

        assertDenied( "A veto in System properties, contradicted in configuration", info );
    }

    /** and symmetrically, since either source may be the one that means to shut things down. */
    public void testDenyAllFromConfigSurvivesADisagreeingSysprop()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA );
        System.setProperty( WHITELIST + ".layerTwo", BETA );

        WhitelistInfo info = collect( fresh(), pcfg( WHITELIST + ".layerOne", DENY_ALL ) );

        assertDenied( "A veto in configuration, contradicted in System properties", info );
    }

    // ------- crossing the precedence between key families -------

    /**
     *  Ordinarily an override replaces the subkeys outright and they are never consulted.
     *  A '[]' among them is consulted anyway: deny is deny, whichever family is in force.
     */
    public void testDenyAllInASubkeyVetoesAnOverrideThatWouldIgnoreIt()
    {
        System.setProperty( WHITELIST + ".layerOne", DENY_ALL );
        System.setProperty( OVERRIDE, ALPHA );

        WhitelistInfo info = collect( fresh() );

        assertDenied( "A veto in a subkey an override would have ignored", info );
        assertEquals( "The override is still what defined the whitelist.",
                      WhitelistInfo.Source.OVERRIDE, info.getSource() );
        assertTrue( "The warning must name the key to edit.",
                    logger.sawWarningContaining( DENY_ALL, WHITELIST + ".layerOne" ) );
    }

    public void testDenyAllInAnOverrideVetoesTheMainWhitelist()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA );
        System.setProperty( OVERRIDE, DENY_ALL );

        assertDenied( "A veto in the override key", collect( fresh() ) );
    }

    /** A deprecated key otherwise suppresses the new keys entirely; a veto still reaches it. */
    public void testDenyAllInASubkeyVetoesADeprecatedWhitelist()
    {
        System.setProperty( DEPRECATED, ALPHA );
        System.setProperty( WHITELIST + ".layerOne", DENY_ALL );

        WhitelistInfo info = collect( migrating() );

        assertDenied( "A veto in a subkey the deprecated key would have ignored", info );
        assertEquals( WhitelistInfo.Source.DEPRECATED, info.getSource() );
    }

    public void testDenyAllInAnOverrideVetoesADeprecatedWhitelist()
    {
        System.setProperty( DEPRECATED, ALPHA );
        System.setProperty( OVERRIDE, DENY_ALL );

        assertDenied( "A veto in the override key, deprecated key in force", collect( migrating() ) );
    }

    /** and the deprecated key can itself carry the veto against the families it suppresses. */
    public void testDenyAllInADeprecatedKeyVetoesTheMainWhitelist()
    {
        System.setProperty( DEPRECATED, DENY_ALL );
        System.setProperty( WHITELIST + ".layerOne", ALPHA );

        assertDenied( "A veto in the deprecated key", collect( migrating() ) );
    }

    // ------- the warning, and its absence -------

    /**
     *  An empty whitelist reached by accident draws a nag naming both ways out. An empty
     *  whitelist the operator asked for does not: they already did the thing the nag would
     *  advise, and telling them to do it again is a contradiction, not guidance.
     */
    public void testAccidentallyEmptyWhitelistIsNaggedAboutAndOffersBothSentinels()
    {
        System.setProperty( WHITELIST + ".layerOne", "" );

        assertDenied( "A blank value", collect( fresh() ) );
        assertTrue( "An accidental empty whitelist should be flagged.",
                    logger.sawWarningContaining( "contains no entries" ) );
        assertTrue( "and should name '*' as the way to open it.",
                    logger.sawWarningContaining( "contains no entries", WILDCARD ) );
        assertTrue( "and '[]' as the way to declare the emptiness deliberate.",
                    logger.sawWarningContaining( "contains no entries", DENY_ALL ) );
    }

    public void testDeliberateDenyAllIsNotNaggedAbout()
    {
        System.setProperty( WHITELIST + ".layerOne", DENY_ALL );

        assertDenied( "An explicit '[]'", collect( fresh() ) );
        assertFalse( "An explicitly requested deny-all must not be told to request a deny-all: " + logger.warnings(),
                     logger.sawWarningContaining( "contains no entries" ) );
    }

    /** and neither is one that arrives by veto from another key family. */
    public void testDenyAllByCrossFamilyVetoIsNotNaggedAbout()
    {
        System.setProperty( WHITELIST + ".layerOne", DENY_ALL );
        System.setProperty( OVERRIDE, ALPHA );

        assertDenied( "A cross-family veto", collect( fresh() ) );
        assertFalse( "The veto already explained itself: " + logger.warnings(),
                     logger.sawWarningContaining( "contains no entries" ) );
    }

    /**
     *  The veto is a security decision, so it must not be contingent on logging. An earlier
     *  revision computed it inside an isLoggable(WARNING) guard, where suppressing warnings
     *  suppressed the correction too.
     */
    public void testDenyAllHoldsWithWarningsDisabled()
    {
        logger.setLoggable( false );

        System.setProperty( WHITELIST + ".layerOne", ALPHA );
        System.setProperty( WHITELIST + ".layerTwo", DENY_ALL );

        assertDenied( "A veto with warnings switched off", collect( fresh() ) );
        assertTrue( "Precondition: nothing was logged.", logger.warnings().isEmpty() );
    }

    /**
     *  Both sentinels are typically the whole value of their key, which is exactly where a
     *  stray space is easiest to leave behind -- and a sentinel that fails to be recognized
     *  fails open or closed rather than merely mismatching one class name.
     */
    public void testDenyAllTokenIsRecognizedDespiteSurroundingWhitespace()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA );
        System.setProperty( WHITELIST + ".layerTwo", "  " + DENY_ALL + "  " );

        assertDenied( "A '[]' with whitespace around it", collect( fresh() ) );
    }

    // ======================= '*' is a wildcard only when it is truly unique =======================

    public void testLoneWildcardIsLeftIntact()
    {
        System.setProperty( WHITELIST + ".layerOne", WILDCARD );

        assertEquals( "A genuine wildcard must survive.",
                      setOf( WILDCARD ), collect( fresh() ).getWhitelist() );
    }

    /**
     *  A '*' among other entries is not a wildcard, and is <b>removed</b> from the resolved
     *  Set rather than merely reported and left for callers to disregard.
     *
     *  <p>Removing it at resolution is what makes the guarantee total. A '*' left in the Set
     *  could be reconstituted as a lone wildcard by a later intersection -- narrow
     *  <code>{*, Alpha}</code> against <code>{*, Beta}</code> and exactly <code>{*}</code>
     *  survives, turning two deployments that each meant to restrict into accept-everything.
     *  EarliestOrNarrowestWhitelistManager relies on that being impossible.</p>
     */
    public void testWildcardAmongOtherEntriesIsRemovedAndWarns()
    {
        System.setProperty( WHITELIST + ".layerOne", WILDCARD );
        System.setProperty( WHITELIST + ".layerTwo", ALPHA );

        WhitelistInfo info = collect( fresh() );

        assertEquals( "A non-unique '*' must not survive into the resolved whitelist.",
                      setOf( ALPHA ), info.getWhitelist() );
        assertTrue( "A non-unique '*' must be warned about.",
                    logger.sawWarningContaining( "not unique" ) );
        assertTrue( "and the warning must name the key family that produced it.",
                    logger.sawWarningContaining( "not unique", WHITELIST ) );
    }

    /**
     *  and having been removed, it cannot be reconstituted by narrowing -- which is the
     *  escalation the removal exists to prevent.
     */
    public void testANonUniqueWildcardCannotBeReconstitutedByNarrowing()
    {
        System.setProperty( WHITELIST + ".layerOne", WILDCARD + "," + ALPHA );

        Set<String> first = collect( fresh() ).getWhitelist();
        assertEquals( setOf( ALPHA ), first );

        System.setProperty( WHITELIST + ".layerOne", WILDCARD + "," + BETA );
        Set<String> second = collect( fresh() ).getWhitelist();

        assertEquals( setOf( BETA ), second );
        assertFalse( "Intersecting these must not be able to yield a lone wildcard.",
                     first.contains( WILDCARD ) || second.contains( WILDCARD ) );
    }

    /**
     *  The escalation the re-check exists for. Two sources each narrow to a different class
     *  while both carry '*'; their intersection is exactly {'*'}, which would read as
     *  accept-everything although neither source asked for that.
     */
    public void testWildcardArisingOnlyFromIntersectionIsRefused()
    {
        System.setProperty( WHITELIST + ".layerOne", ALPHA + "," + WILDCARD );

        WhitelistInfo info = collect( fresh(), pcfg( WHITELIST + ".layerOne", BETA + "," + WILDCARD ) );

        assertDenied( "A '*' that is merely the residue of two disagreeing lists", info );
        assertTrue( "and the refusal must be explained.",
                    logger.sawWarningContaining( "DENY ALL" ) );
    }

    /** whereas a '*' both sources actually agree on is honored. */
    public void testWildcardAgreedByBothSourcesIsHonored()
    {
        System.setProperty( WHITELIST + ".layerOne", WILDCARD );

        assertEquals( setOf( WILDCARD ),
                      collect( fresh(), pcfg( WHITELIST + ".layerOne", WILDCARD ) ).getWhitelist() );
    }

    /** as is one only a single source mentions -- there is no disagreement to be suspicious of. */
    public void testWildcardFromASingleSourceIsHonored()
    {
        System.setProperty( WHITELIST + ".layerOne", WILDCARD );

        assertEquals( setOf( WILDCARD ),
                      collect( fresh(), pcfg( WHITELIST + ".unrelated", "" ) ).getWhitelist() );
    }

    /**
     *  A key that contributed nothing must not veto a wildcard another key vouched for. An
     *  earlier revision asked the re-check about every subkey, so a subkey blank in both
     *  sources reported itself as an inconsistent intersection and emptied the whitelist.
     */
    public void testSilentSubkeyDoesNotVetoAGenuineWildcard()
    {
        System.setProperty( WHITELIST + ".layerOne", WILDCARD );
        System.setProperty( WHITELIST + ".layerTwo", "" );

        WhitelistInfo info = collect( fresh(),
                                      pcfg( WHITELIST + ".layerOne", WILDCARD, WHITELIST + ".layerTwo", "" ) );

        assertEquals( "A blank key is silent, not dissenting.", setOf( WILDCARD ), info.getWhitelist() );
    }

    public void testWildcardIsRecognizedDespiteSurroundingWhitespace()
    {
        System.setProperty( WHITELIST + ".layerOne", " " + WILDCARD + " " );

        assertEquals( "A '*' with whitespace around it is still the wildcard.",
                      setOf( WILDCARD ), collect( fresh() ).getWhitelist() );
    }

    /**
     *  A leading comma yields an empty-string entry. It can never match a class name, so it
     *  does no harm on its own -- but it is an entry, so it costs the wildcard its
     *  uniqueness, and that must be said out loud rather than silently dropping the '*'.
     */
    public void testAnInertEmptyEntryCostsTheWildcardItsUniquenessAudibly()
    {
        System.setProperty( WHITELIST + ".layerOne", "," + WILDCARD );

        WhitelistInfo info = collect( fresh() );

        assertEquals( "The '*' is stripped, leaving only the inert entry -- so this whitelist " +
                      "permits nothing, rather than everything.",
                      setOf( "" ), info.getWhitelist() );
        assertTrue( "The operator must be told why their wildcard stopped working.",
                    logger.sawWarningContaining( "not unique" ) );
    }

    /** The refusal, like the veto, is a security decision and cannot depend on log level. */
    public void testWildcardRefusalHoldsWithWarningsDisabled()
    {
        logger.setLoggable( false );

        System.setProperty( WHITELIST + ".layerOne", ALPHA + "," + WILDCARD );

        WhitelistInfo info = collect( fresh(), pcfg( WHITELIST + ".layerOne", BETA + "," + WILDCARD ) );

        assertDenied( "A spurious wildcard with warnings switched off", info );
        assertTrue( "Precondition: nothing was logged.", logger.warnings().isEmpty() );
    }
}
