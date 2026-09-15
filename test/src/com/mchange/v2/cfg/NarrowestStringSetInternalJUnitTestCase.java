package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestStringSetFromStringListSyspropsPropertiesConfig;
import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;
import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig;
import static com.mchange.v2.cfg.PropertiesConfigUtils.narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken;

/**
 *  The two comma-separated-list resolvers in PropertiesConfigUtils, as general utilities.
 *
 *  <p>Both read a key from System properties and from a supplied {@link PropertiesConfig},
 *  and where both sources name it and disagree, take the <i>intersection</i> -- a value only
 *  one source vouches for is not vouched for. The per-key variant then unions across keys, so
 *  independent layers can each contribute to one composite list.</p>
 *
 *  <p>The <code>alwaysRetainToken</code> overloads exempt one designated String from that
 *  intersection: if either source names it, it survives. WhitelistManager uses this for its
 *  <code>[]</code> deny-all sentinel, because intersection is conservative for permissions but
 *  not for denials. Nothing about the mechanism is specific to that, though, and these tests
 *  deliberately use tokens with no whitelist meaning -- an ordinary-looking class name among
 *  them -- so the contract is pinned as the general one it claims to be. (An earlier revision
 *  hardcoded <code>"[]"</code> in place of the caller's token; nothing noticed, because the
 *  one caller that passed a token passed exactly that.)</p>
 *
 *  <p>Declared in-package because BasicMultiPropertiesConfig is not public, and a second
 *  source to disagree with is the whole point.</p>
 */
public class NarrowestStringSetInternalJUnitTestCase extends TestCase
{
    private final static String PFX = "com.mchange.v2.cfg.junit.narrowest";
    private final static String KEY = PFX + ".key";
    private final static String KEY2 = PFX + ".other";

    /** deliberately not '*' or '[]' -- nothing here should depend on the whitelist vocabulary */
    private final static String TOKEN = "<KEEP>";

    private final static String ALPHA = "com.example.Alpha";
    private final static String BETA  = "com.example.Beta";
    private final static String GAMMA = "com.example.Gamma";

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
            if ( k.startsWith( PFX ) )
                System.setProperty( k, saved.getProperty( k ) );
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

    private static PropertiesConfig pcfg( String... keysAndValues )
    {
        Properties props = new Properties();
        for ( int i = 0; i < keysAndValues.length; i += 2 )
            props.setProperty( keysAndValues[i], keysAndValues[i + 1] );
        return new BasicMultiPropertiesConfig( "/notional-test-resource", props );
    }

    private static Set<String> setOf( String... elems )
    { return new HashSet<String>( Arrays.asList( elems ) ); }

    private Set<String> narrowest( PropertiesConfig pcfg )
    { return narrowestStringSetFromStringListSyspropsPropertiesConfig( KEY, pcfg, logger ); }

    private Set<String> narrowest( PropertiesConfig pcfg, String token )
    { return narrowestStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken( KEY, pcfg, token, logger ); }

    private Set<String> union( PropertiesConfig pcfg, String token, String... keys )
    {
        return narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfigWithAlwaysRetainToken(
                   setOf( keys ), pcfg, token, logger );
    }

    // ==================== single key, no token ====================

    /** null, not empty: callers distinguish "unconfigured" from "configured to nothing". */
    public void testNullWhenTheKeyIsAbsentFromBothSources()
    {
        assertNull( narrowest( null ) );
        assertNull( narrowest( pcfg( KEY2, ALPHA ) ) );
    }

    public void testSyspropsAloneIsTakenWhole()
    {
        System.setProperty( KEY, ALPHA + "," + BETA );

        assertEquals( setOf( ALPHA, BETA ), narrowest( null ) );
        assertEquals( "No config value means no disagreement to narrow.",
                      setOf( ALPHA, BETA ), narrowest( pcfg( KEY2, GAMMA ) ) );
    }

    public void testConfigAloneIsTakenWhole()
    { assertEquals( setOf( ALPHA, BETA ), narrowest( pcfg( KEY, ALPHA + "," + BETA ) ) ); }

    /** Whitespace around commas is incidental. */
    public void testWhitespaceAroundCommasIsIgnored()
    {
        System.setProperty( KEY, ALPHA + "  ,   " + BETA );

        assertEquals( setOf( ALPHA, BETA ), narrowest( null ) );
    }

    /**
     *  and so is whitespace at the two ends of the whole value. The split trims only around
     *  commas, so without an explicit trim the first and last entries kept theirs -- and a
     *  properties file makes that easy to do by accident, since Properties.load strips
     *  leading whitespace after the '=' but keeps trailing. The resulting entry looks
     *  correct in the file and matches no class name.
     */
    public void testWhitespaceAtTheEndsOfTheValueIsStripped()
    {
        System.setProperty( KEY, "  " + ALPHA + " ,  " + BETA + "  " );
        assertEquals( setOf( ALPHA, BETA ), narrowest( null ) );

        System.setProperty( KEY, " " + ALPHA + " " );
        assertEquals( "A sole entry is trimmed at both ends.", setOf( ALPHA ), narrowest( null ) );

        System.setProperty( KEY, "\t" + ALPHA + "\t" );
        assertEquals( "Tabs, not just spaces.", setOf( ALPHA ), narrowest( null ) );
    }

    public void testBlankAndEmptyValuesAreEmptyLists()
    {
        System.setProperty( KEY, "   " );
        assertTrue( "A blank value is an empty list.", narrowest( null ).isEmpty() );

        System.setProperty( KEY, "" );
        assertTrue( "and so is an empty one.", narrowest( null ).isEmpty() );

        System.setProperty( KEY, " , " );
        assertTrue( "and so is one that is nothing but separators.", narrowest( null ).isEmpty() );
    }

    /**
     *  An interior double comma does yield an empty-string element. It is harmless: it can
     *  never match a class name, and callers that care about uniqueness see it as the
     *  ordinary extra entry it is.
     */
    public void testInteriorDoubleCommaYieldsAnInertEmptyElement()
    {
        System.setProperty( KEY, ALPHA + ",," + BETA );

        assertEquals( setOf( ALPHA, "", BETA ), narrowest( null ) );
    }

    public void testAgreeingSourcesAgreeSilently()
    {
        System.setProperty( KEY, ALPHA + "," + BETA );

        assertEquals( setOf( ALPHA, BETA ), narrowest( pcfg( KEY, BETA + "," + ALPHA ) ) );
        assertTrue( "Agreement is not worth warning about: " + logger.warnings(),
                    logger.warningsContaining( "Inconsistent values" ).isEmpty() );
    }

    public void testDisagreeingSourcesIntersectAndWarn()
    {
        System.setProperty( KEY, ALPHA + "," + BETA );

        assertEquals( setOf( BETA ), narrowest( pcfg( KEY, BETA + "," + GAMMA ) ) );
        assertTrue( "A narrowed list must be explained.",
                    logger.sawWarningContaining( "Inconsistent values", KEY ) );
    }

    public void testWhollyDisagreeingSourcesYieldAnEmptySet()
    {
        System.setProperty( KEY, ALPHA );

        Set<String> out = narrowest( pcfg( KEY, GAMMA ) );
        assertNotNull( "Empty, but configured -- not null.", out );
        assertTrue( out.isEmpty() );
    }

    public void testResultIsUnmodifiable()
    {
        System.setProperty( KEY, ALPHA + "," + BETA );

        for ( Set<String> out : Arrays.asList( narrowest( null ),
                                               narrowest( pcfg( KEY, ALPHA + "," + BETA ) ),
                                               narrowest( pcfg( KEY, BETA + "," + GAMMA ) ) ) )
        {
            try
            {
                out.add( "com.example.Sneaky" );
                fail( "Callers must not be able to widen a resolved list in place: " + out );
            }
            catch ( UnsupportedOperationException expected )
            {}
        }
    }

    // ==================== single key, with a token ====================

    /**
     *  The point of the overload: a designated token survives an intersection that would
     *  otherwise drop it, from whichever source names it.
     */
    public void testTokenSurvivesFromSysprops()
    {
        System.setProperty( KEY, ALPHA + "," + TOKEN );

        assertEquals( setOf( TOKEN ), narrowest( pcfg( KEY, BETA ), TOKEN ) );
    }

    public void testTokenSurvivesFromConfig()
    {
        System.setProperty( KEY, ALPHA );

        assertEquals( setOf( TOKEN ), narrowest( pcfg( KEY, BETA + "," + TOKEN ), TOKEN ) );
    }

    public void testTokenSurvivesAlongsideAgreedEntries()
    {
        System.setProperty( KEY, ALPHA + "," + BETA + "," + TOKEN );

        assertEquals( "Retention adds the token; it does not replace the intersection.",
                      setOf( BETA, TOKEN ), narrowest( pcfg( KEY, BETA + "," + GAMMA ), TOKEN ) );
    }

    /** The token is never conjured: retention keeps what a source said, it does not invent it. */
    public void testTokenAbsentFromBothSourcesIsNotAdded()
    {
        System.setProperty( KEY, ALPHA + "," + BETA );

        assertEquals( setOf( BETA ), narrowest( pcfg( KEY, BETA + "," + GAMMA ), TOKEN ) );
    }

    /** A null token means no retention, and must not be inserted as a null element. */
    public void testNullTokenRetainsNothing()
    {
        System.setProperty( KEY, ALPHA + "," + BETA );

        Set<String> out = narrowest( pcfg( KEY, BETA + "," + GAMMA ), null );
        assertEquals( setOf( BETA ), out );
        assertFalse( "No null element may leak into the result.", out.contains( null ) );
    }

    /**
     *  Any String may be the token -- there is nothing special about the sentinels
     *  WhitelistManager happens to use. An ordinary class name works identically.
     */
    public void testAnArbitraryTokenWorksIncludingAnOrdinaryClassName()
    {
        System.setProperty( KEY, ALPHA + "," + GAMMA );

        assertEquals( "A plain class name as the retained token.",
                      setOf( GAMMA ), narrowest( pcfg( KEY, BETA + "," + GAMMA ), GAMMA ) );

        System.setProperty( KEY, ALPHA + ",@@ALWAYS@@" );
        assertEquals( setOf( "@@ALWAYS@@" ), narrowest( pcfg( KEY, BETA + ",@@ALWAYS@@" ), "@@ALWAYS@@" ) );
    }

    /** Retention only bears on disagreement; where one source is silent there is nothing to narrow. */
    public void testTokenIsIrrelevantWhenOnlyOneSourceNamesTheKey()
    {
        System.setProperty( KEY, ALPHA );

        assertEquals( setOf( ALPHA ), narrowest( null, TOKEN ) );
        assertEquals( setOf( ALPHA ), narrowest( pcfg( KEY2, BETA ), TOKEN ) );
    }

    // ==================== union across keys ====================

    public void testUnionIsNullWhenNoKeyIsPresentAnywhere()
    {
        assertNull( narrowestPerKeyUnionAcrossKeysStringSetFromStringListSyspropsPropertiesConfig(
                        setOf( KEY, KEY2 ), null, logger ) );
    }

    public void testUnionAcrossKeys()
    {
        System.setProperty( KEY, ALPHA );
        System.setProperty( KEY2, BETA );

        assertEquals( setOf( ALPHA, BETA ), union( null, null, KEY, KEY2 ) );
    }

    /** Intersection is per key, and happens before the union -- one silent source cannot cancel another key. */
    public void testIntersectionAppliesPerKeyBeforeTheUnion()
    {
        System.setProperty( KEY, ALPHA + "," + BETA );
        System.setProperty( KEY2, GAMMA );

        assertEquals( "KEY narrows to BETA; KEY2, named only in sysprops, contributes whole.",
                      setOf( BETA, GAMMA ), union( pcfg( KEY, BETA ), null, KEY, KEY2 ) );
    }

    /** and the token is honored per key, so one key's token reaches the composite result. */
    public void testTokenIsHonoredPerKeyWithinTheUnion()
    {
        System.setProperty( KEY, ALPHA + "," + TOKEN );
        System.setProperty( KEY2, GAMMA );

        assertEquals( setOf( TOKEN, GAMMA ), union( pcfg( KEY, BETA ), TOKEN, KEY, KEY2 ) );
    }

    public void testUnionWithNoTokenDropsTheSameStringOrdinarily()
    {
        System.setProperty( KEY, ALPHA + "," + TOKEN );
        System.setProperty( KEY2, GAMMA );

        assertEquals( "Without designation the token is just another String, and intersection drops it.",
                      setOf( GAMMA ), union( pcfg( KEY, BETA ), null, KEY, KEY2 ) );
    }

    public void testUnionSkipsKeysAbsentFromBothSources()
    {
        System.setProperty( KEY, ALPHA );

        assertEquals( setOf( ALPHA ), union( null, TOKEN, KEY, KEY2, PFX + ".neverSet" ) );
    }
}
