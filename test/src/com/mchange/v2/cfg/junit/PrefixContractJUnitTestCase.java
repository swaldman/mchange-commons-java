package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import com.mchange.v2.cfg.MConfig;
import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v3.hocon.HoconMultiPropertiesConfig;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Properties;
import java.util.TreeMap;

/**
 *  The by-prefix contract, asserted against EVERY implementation of it rather than against
 *  one.
 *
 *  <p>MultiPropertiesConfig.getPropertiesByPrefix has three implementations --
 *  BasicMultiPropertiesConfig, CombinedMultiPropertiesConfig (pure delegation), and
 *  HoconMultiPropertiesConfig -- and for most of its history the abstract declaration promised
 *  only that the result would be non-null and that "" meant everything. Under that silence the
 *  implementations drifted: Basic indexes every key under itself, so a bare {@code a.b=v}
 *  answers getPropertiesByPrefix("a.b") with that very entry, while Hocon filtered on
 *  {@code pfx + '.'} and answered empty.</p>
 *
 *  <p>The silence is what let it happen, so the contract is now written down on the abstract
 *  declaration and these cases hold both implementations to it. The divergence was latent
 *  rather than live -- nothing in this library constructs a HoconMultiPropertiesConfig, and the
 *  {@code hocon:} identifier converts to Properties and lands in a Basic -- but "latent" is a
 *  statement about today's callers, not about the contract.</p>
 */
public final class PrefixContractJUnitTestCase extends TestCase
{
    // ------------------------------------------------------------------ fixtures

    private static MultiPropertiesConfig basic( String... kvs )
    {
        Properties p = new Properties();
        for ( int i = 0; i < kvs.length; i += 2 )
            p.setProperty( kvs[i], kvs[i+1] );
        return MultiPropertiesConfig.fromProperties( "/notional-basic", p );
    }

    private static MultiPropertiesConfig hocon( String hoconText )
    {
        Config config = ConfigFactory.parseString( hoconText );
        return new HoconMultiPropertiesConfig( "notional-hocon", config );
    }

    private static String dump( Properties p )
    { return new TreeMap<Object,Object>( p ).toString(); }

    // ------------------------------------------- the contract, implementation-independent

    /**
     *  A key is a prefix of itself, so a bare key appears under its own name.
     *
     *  <p>This is the clause the two implementations disagreed about. It matters because
     *  WhitelistManager walks a prefix's keys to find a composite whitelist, and its fallback
     *  path over live System properties spells the rule out as
     *  {@code k.equals(baseKey) || k.startsWith(baseKey + ".")} -- written, evidently, to match
     *  what Basic does.</p>
     */
    private static void assertExactKeyIsIncluded( String label, MultiPropertiesConfig cfg )
    {
        Properties under = cfg.getPropertiesByPrefix( "a.b" );
        assertEquals( label + ": a bare key must appear under its own name, got " + dump( under ),
                      1, under.size() );
        assertEquals( label + ": and keyed by its FULL key, got " + dump( under ),
                      "v", under.getProperty( "a.b" ) );
        assertTrue( label + ": containsKey(pfx) is how a caller detects it", under.containsKey( "a.b" ) );
    }

    /** Results are keyed by full key, not by the remainder after the prefix. */
    private static void assertKeysAreFullKeys( String label, MultiPropertiesConfig cfg )
    {
        Properties under = cfg.getPropertiesByPrefix( "a.b" );
        assertEquals( label + ": " + dump( under ), "1", under.getProperty( "a.b.c" ) );
        assertNull( label + ": the suffix alone must not be a key, got " + dump( under ),
                    under.getProperty( "c" ) );
    }

    /** A prefix matches whole dot-separated tokens, never bare characters. */
    private static void assertPrefixMatchesByToken( String label, MultiPropertiesConfig cfg )
    {
        Properties under = cfg.getPropertiesByPrefix( "a.b" );
        assertEquals( label + ": " + dump( under ), "mine", under.getProperty( "a.b.c" ) );
        assertNull( label + ": a sibling sharing only characters must not match, got " + dump( under ),
                    under.getProperty( "a.bx" ) );
        assertEquals( label + ": exactly one key is beneath a.b, got " + dump( under ),
                      1, under.size() );
    }

    /** "" is everything; an unknown prefix is empty rather than null. */
    private static void assertEmptyAndUnknownPrefixes( String label, MultiPropertiesConfig cfg )
    {
        Properties all = cfg.getPropertiesByPrefix( "" );
        assertEquals( label + ": \"\" returns everything, got " + dump( all ), 2, all.size() );

        Properties none = cfg.getPropertiesByPrefix( "no.such.prefix" );
        assertNotNull( label + ": an unknown prefix must yield empty Properties, not null", none );
        assertEquals( label + ": " + dump( none ), 0, none.size() );
    }

    // ------------------------------------------------------- BasicMultiPropertiesConfig

    public void testExactKeyIsIncludedInBasic()
    { assertExactKeyIsIncluded( "Basic", basic( "a.b", "v" ) ); }

    public void testKeysAreFullKeysInBasic()
    { assertKeysAreFullKeys( "Basic", basic( "a.b.c", "1" ) ); }

    public void testPrefixMatchesByTokenInBasic()
    { assertPrefixMatchesByToken( "Basic", basic( "a.b.c", "mine", "a.bx", "not-mine" ) ); }

    public void testEmptyAndUnknownPrefixesInBasic()
    { assertEmptyAndUnknownPrefixes( "Basic", basic( "a.b.c", "1", "dotless", "2" ) ); }

    // -------------------------------------------------------- HoconMultiPropertiesConfig

    public void testExactKeyIsIncludedInHocon()
    { assertExactKeyIsIncluded( "Hocon", hocon( "a.b = v" ) ); }

    public void testKeysAreFullKeysInHocon()
    { assertKeysAreFullKeys( "Hocon", hocon( "a.b.c = 1" ) ); }

    public void testPrefixMatchesByTokenInHocon()
    { assertPrefixMatchesByToken( "Hocon", hocon( "a.b.c = mine\na.bx = not-mine" ) ); }

    public void testEmptyAndUnknownPrefixesInHocon()
    { assertEmptyAndUnknownPrefixes( "Hocon", hocon( "a.b.c = 1\ndotless = 2" ) ); }

    // ---------------------------------------------------- the two must agree, key for key

    /**
     *  The same logical configuration, built both ways, must answer the same question the same
     *  way. Asserted over every prefix either implementation could be asked about, because a
     *  divergence at any one of them is a divergence in the contract.
     */
    public void testBasicAndHoconAgreeOverEveryPrefix()
    {
        MultiPropertiesConfig b = basic( "a.b", "scalar", "x.y.z", "deep", "dotless", "flat" );
        MultiPropertiesConfig h = hocon( "a.b = scalar\nx.y.z = deep\ndotless = flat" );

        String[] prefixes = { "", "a", "a.b", "x", "x.y", "x.y.z", "dotless", "nope" };
        for ( String pfx : prefixes )
        {
            Properties bp = b.getPropertiesByPrefix( pfx );
            Properties hp = h.getPropertiesByPrefix( pfx );
            assertEquals( "the two implementations disagree at prefix '" + pfx + "': Basic=" +
                          dump( bp ) + " Hocon=" + dump( hp ),
                          dump( bp ), dump( hp ) );
        }
    }

    /**
     *  Why the blast radius of aligning Hocon is small, pinned as a fact rather than assumed.
     *
     *  <p>In HOCON a path is either a scalar or an object, never both -- the later assignment
     *  replaces the earlier wholesale. So the case Basic can represent, an exact key with
     *  subkeys beneath it, is unreachable from a HOCON document. Including the exact key can
     *  therefore only turn an EMPTY Hocon result into a one-entry result; it can never alter a
     *  result that already has entries.</p>
     */
    public void testHoconCannotHoldAScalarAndAnObjectAtOnePath()
    {
        MultiPropertiesConfig scalarThenObject = hocon( "a.b = scalar\na.b.c = sub" );
        assertNull( "the scalar must be gone, replaced by the object",
                    scalarThenObject.getProperty( "a.b" ) );
        assertEquals( "sub", scalarThenObject.getProperty( "a.b.c" ) );

        MultiPropertiesConfig objectThenScalar = hocon( "a.b.c = sub\na.b = scalar" );
        assertEquals( "scalar", objectThenScalar.getProperty( "a.b" ) );
        assertNull( "and in this order the object is gone instead",
                    objectThenScalar.getProperty( "a.b.c" ) );
    }

    // ------------------------------------------------------------------- the documented use

    /**
     *  The recipe the javadoc gives for telling a bare key from a populated scope, which is the
     *  thing a caller actually needs and the thing {@code size() > 0} alone conflates.
     */
    public void testDistinguishingABareKeyFromAPopulatedScope()
    {
        assertScopeReading( "bare key only",  basic( "a.b", "v" ),                       true,  false );
        assertScopeReading( "subkeys only",   basic( "a.b.c", "1" ),                     false, true  );
        assertScopeReading( "both",           basic( "a.b", "v", "a.b.c", "1" ),         true,  true  );
        assertScopeReading( "neither",        basic( "unrelated", "x" ),                 false, false );

        // HOCON cannot express "both", per the case above; the other three it can.
        assertScopeReading( "hocon bare key", hocon( "a.b = v" ),     true,  false );
        assertScopeReading( "hocon subkeys",  hocon( "a.b.c = 1" ),   false, true  );
        assertScopeReading( "hocon neither",  hocon( "unrelated = x" ), false, false );
    }

    private static void assertScopeReading( String label, MultiPropertiesConfig cfg,
                                            boolean expectBareKeySet, boolean expectSubkeys )
    {
        Properties under      = cfg.getPropertiesByPrefix( "a.b" );
        boolean    bareKeySet = under.containsKey( "a.b" );
        boolean    hasSubkeys = under.size() > ( bareKeySet ? 1 : 0 );

        assertEquals( label + ": bare key set? " + dump( under ), expectBareKeySet, bareKeySet );
        assertEquals( label + ": subkeys present? " + dump( under ), expectSubkeys, hasSubkeys );

        assertEquals( label + ": getProperty(pfx) must agree with containsKey(pfx)",
                      expectBareKeySet, cfg.getProperty( "a.b" ) != null );
    }

    // -------------------------------------------------------------------------- combine()

    /**
     *  And the reason the divergence stayed latent: combine() flattens whatever it is given
     *  into a Basic, by resource path rather than by prefix, so a Hocon config contributed as
     *  a refresh override (which is what c3p0-play does) gets Basic's prefix semantics
     *  regardless. This is the path that must not regress.
     */
    public void testAHoconConfigCombinedTakesBasicPrefixSemantics()
    {
        MultiPropertiesConfig combined = MConfig.combine(
            new MultiPropertiesConfig[] { hocon( "a.b = scalar" ) } );

        Properties under = combined.getPropertiesByPrefix( "a.b" );
        assertEquals( "combined, the exact key is present whatever the source implementation did: "
                      + dump( under ), 1, under.size() );
        assertEquals( "scalar", under.getProperty( "a.b" ) );
    }
}
