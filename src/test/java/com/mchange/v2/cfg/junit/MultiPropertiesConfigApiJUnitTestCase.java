package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.MConfig;
import com.mchange.v2.cfg.MultiPropertiesConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 *  The parts of the MultiPropertiesConfig surface that do not depend on what is
 *  present on the classpath, so they need no scenario ClassLoader: by-prefix and
 *  by-key indexing, by-resource-path access, programmatically supplied Properties,
 *  and combine().
 */
public final class MultiPropertiesConfigApiJUnitTestCase extends TestCase
{
    private static Properties props( String... kvs )
    {
        Properties p = new Properties();
        for ( int i = 0; i < kvs.length; i += 2 )
            p.setProperty( kvs[i], kvs[i+1] );
        return p;
    }

    private static MultiPropertiesConfig prefixFixture()
    {
        return MultiPropertiesConfig.fromProperties(
            props( "x.y.z.key", "deep",
                   "x.y.other",  "mid",
                   "x.top",      "shallow",
                   "dotless",    "flat" ) );
    }

    /** The special prefix "" returns all the Properties, as documented. */
    public void testEmptyPrefixReturnsEverything()
    {
        Properties all = prefixFixture().getPropertiesByPrefix( "" );
        assertEquals( 4, all.size() );
        assertEquals( "deep",    all.getProperty( "x.y.z.key" ) );
        assertEquals( "flat",    all.getProperty( "dotless" ) );
        assertEquals( "shallow", all.getProperty( "x.top" ) );
    }

    /** A key is indexed under every '.'-separated prefix of itself. */
    public void testKeysAreIndexedUnderEveryAncestorPrefix()
    {
        MultiPropertiesConfig mpc = prefixFixture();

        assertEquals( "deep", mpc.getPropertiesByPrefix( "x.y.z" ).getProperty( "x.y.z.key" ) );
        assertEquals( "deep", mpc.getPropertiesByPrefix( "x.y" ).getProperty( "x.y.z.key" ) );
        assertEquals( "deep", mpc.getPropertiesByPrefix( "x" ).getProperty( "x.y.z.key" ) );

        // "x.y" also carries its own direct child
        assertEquals( "mid", mpc.getPropertiesByPrefix( "x.y" ).getProperty( "x.y.other" ) );
        assertEquals( 2, mpc.getPropertiesByPrefix( "x.y" ).size() );

        // "x" carries everything beneath it, but nothing outside it
        assertEquals( 3, mpc.getPropertiesByPrefix( "x" ).size() );
        assertNull( mpc.getPropertiesByPrefix( "x" ).getProperty( "dotless" ) );
    }

    /** A dotless key lives only under the "" prefix. */
    public void testDotlessKeyOnlyUnderEmptyPrefix()
    {
        MultiPropertiesConfig mpc = prefixFixture();
        assertEquals( "flat", mpc.getPropertiesByPrefix( "" ).getProperty( "dotless" ) );
        assertEquals( "an unknown prefix yields empty Properties, not null",
                      0, mpc.getPropertiesByPrefix( "dotless" ).size() );
    }

    /** An unknown prefix yields empty Properties rather than null. */
    public void testUnknownPrefixYieldsEmptyProperties()
    {
        Properties p = prefixFixture().getPropertiesByPrefix( "no.such.prefix" );
        assertNotNull( p );
        assertEquals( 0, p.size() );
    }

    /** fromProperties uses a documented sentinel notional resource path. */
    public void testFromPropertiesUsesSentinelPath()
    {
        MultiPropertiesConfig mpc = MultiPropertiesConfig.fromProperties( props( "a", "1" ) );
        assertEquals( Arrays.asList( "PROGRAMMATICALLY_SUPPLIED_PROPERTIES" ),
                      Arrays.asList( mpc.getPropertiesResourcePaths() ) );
        assertEquals( "1", mpc.getProperty( "a" ) );
        assertEquals( "1", mpc.getPropertiesByResourcePath( "PROGRAMMATICALLY_SUPPLIED_PROPERTIES" )
                              .getProperty( "a" ) );
    }

    /** An explicit notional resource path is honored. */
    public void testFromPropertiesWithExplicitPath()
    {
        MultiPropertiesConfig mpc = MultiPropertiesConfig.fromProperties( "/notional", props( "a", "1" ) );
        assertEquals( Arrays.asList( "/notional" ), Arrays.asList( mpc.getPropertiesResourcePaths() ) );
        assertEquals( "1", mpc.getPropertiesByResourcePath( "/notional" ).getProperty( "a" ) );
    }

    /** An unknown resource path yields empty Properties rather than null. */
    public void testUnknownResourcePathYieldsEmptyProperties()
    {
        Properties p = MultiPropertiesConfig.fromProperties( props( "a", "1" ) )
                                            .getPropertiesByResourcePath( "/nope" );
        assertNotNull( p );
        assertEquals( 0, p.size() );
    }

    /** MConfig.combine: later entries in the array override earlier ones. */
    public void testCombineLaterOverridesEarlier()
    {
        MultiPropertiesConfig first  = MultiPropertiesConfig.fromProperties( "/first",  props( "shared", "from-first",  "only.first",  "yes" ) );
        MultiPropertiesConfig second = MultiPropertiesConfig.fromProperties( "/second", props( "shared", "from-second", "only.second", "yes" ) );

        MultiPropertiesConfig combined = MConfig.combine( new MultiPropertiesConfig[] { first, second } );

        assertEquals( "from-second", combined.getProperty( "shared" ) );
        assertEquals( "yes", combined.getProperty( "only.first" ) );
        assertEquals( "yes", combined.getProperty( "only.second" ) );
        assertEquals( Arrays.asList( "/first", "/second" ),
                      Arrays.asList( combined.getPropertiesResourcePaths() ) );
    }

    /** Reversing the array reverses which definition wins. */
    public void testCombineIsOrderSensitive()
    {
        MultiPropertiesConfig first  = MultiPropertiesConfig.fromProperties( "/first",  props( "shared", "from-first" ) );
        MultiPropertiesConfig second = MultiPropertiesConfig.fromProperties( "/second", props( "shared", "from-second" ) );

        assertEquals( "from-first",
                      MConfig.combine( new MultiPropertiesConfig[] { second, first } ).getProperty( "shared" ) );
    }

    // ------------------------------------------- combining configs that share a resource path

    private final static String SHARED = "/shared-path.properties";

    /** Two configs, both naming SHARED, differing on "shared" and each with a key of its own. */
    private static MultiPropertiesConfig sharedFirst()
    { return MultiPropertiesConfig.fromProperties( SHARED, props( "shared", "from-first",  "only.first",  "yes", "grp.a", "first-a", "grp.b", "first-b" ) ); }

    private static MultiPropertiesConfig sharedSecond()
    { return MultiPropertiesConfig.fromProperties( SHARED, props( "shared", "from-second", "only.second", "yes", "grp.b", "second-b" ) ); }

    /**
     *  Configs that name the SAME resource path merge rather than shadow.
     *
     *  <p>The existing combine tests use distinct paths, where nothing has to be reconciled
     *  under one name. When two configs claim one path, the union of their keys must survive
     *  and only genuine conflicts get resolved -- otherwise the later config would silently
     *  take the path over and everything unique to the earlier one would vanish.</p>
     *
     *  <p>The path itself is reported once, not once per contributing config.</p>
     */
    public void testCombineMergesConfigsSharingAResourcePath()
    {
        MultiPropertiesConfig combined =
            MConfig.combine( new MultiPropertiesConfig[] { sharedFirst(), sharedSecond() } );

        assertEquals( "the conflict resolves in favor of the later config", "from-second", combined.getProperty( "shared" ) );
        assertEquals( "a key only the earlier config had must survive",     "yes",         combined.getProperty( "only.first" ) );
        assertEquals( "and one only the later config had",                  "yes",         combined.getProperty( "only.second" ) );

        assertEquals( "the shared path should be listed once",
                      Arrays.asList( SHARED ), Arrays.asList( combined.getPropertiesResourcePaths() ) );

        // the same reconciliation, seen through the other two indices
        Properties byPath = combined.getPropertiesByResourcePath( SHARED );
        assertEquals( "from-second", byPath.getProperty( "shared" ) );
        assertEquals( "yes",         byPath.getProperty( "only.first" ) );
        assertEquals( "yes",         byPath.getProperty( "only.second" ) );

        Properties byPrefix = combined.getPropertiesByPrefix( "grp" );
        assertEquals( "untouched by the later config", "first-a",  byPrefix.getProperty( "grp.a" ) );
        assertEquals( "overridden by the later config", "second-b", byPrefix.getProperty( "grp.b" ) );
    }

    /** Reversing the array reverses the winner here too, for every index. */
    public void testCombineOfASharedPathIsOrderSensitive()
    {
        MultiPropertiesConfig combined =
            MConfig.combine( new MultiPropertiesConfig[] { sharedSecond(), sharedFirst() } );

        assertEquals( "from-first", combined.getProperty( "shared" ) );
        assertEquals( "yes", combined.getProperty( "only.first" ) );
        assertEquals( "yes", combined.getProperty( "only.second" ) );
        assertEquals( "from-first", combined.getPropertiesByResourcePath( SHARED ).getProperty( "shared" ) );
        assertEquals( "first-b",    combined.getPropertiesByPrefix( "grp" ).getProperty( "grp.b" ) );
    }

    /** Three configs on one path: the last one still wins, and earlier contributions still stand. */
    public void testCombineMergesMoreThanTwoOnOnePath()
    {
        MultiPropertiesConfig third = MultiPropertiesConfig.fromProperties( SHARED, props( "shared", "from-third" ) );

        MultiPropertiesConfig combined =
            MConfig.combine( new MultiPropertiesConfig[] { sharedFirst(), sharedSecond(), third } );

        assertEquals( "from-third", combined.getProperty( "shared" ) );
        assertEquals( "yes",        combined.getProperty( "only.first" ) );
        assertEquals( "yes",        combined.getProperty( "only.second" ) );
        assertEquals( "second-b",   combined.getPropertiesByPrefix( "grp" ).getProperty( "grp.b" ) );
    }

    /**
     *  A shared path interleaved with a distinct one still resolves by array position.
     *
     *  <p>Deduplicating the path list means a repeated path can only sit in one place, so the
     *  place has to be the one that preserves precedence -- its LAST occurrence. Were it pinned
     *  at its first appearance instead, a config named after it in the array would be
     *  overridden by one named before it.</p>
     */
    public void testASharedPathIsOrderedByItsLastOccurrence()
    {
        MultiPropertiesConfig other = MultiPropertiesConfig.fromProperties( "/other", props( "shared", "from-other" ) );

        MultiPropertiesConfig sharedLast =
            MConfig.combine( new MultiPropertiesConfig[] { sharedFirst(), other, sharedSecond() } );
        assertEquals( "the last-named config wins, though its path appeared earlier too",
                      "from-second", sharedLast.getProperty( "shared" ) );

        MultiPropertiesConfig otherLast =
            MConfig.combine( new MultiPropertiesConfig[] { sharedFirst(), sharedSecond(), other } );
        assertEquals( "and with the distinct path last, it wins instead",
                      "from-other", otherLast.getProperty( "shared" ) );

        assertEquals( "both paths are listed, each once", 2, otherLast.getPropertiesResourcePaths().length );
    }

    /**
     *  Properties may legally hold non-String keys and values (via put rather than
     *  setProperty). Those entries are skipped with a WARNING rather than blowing up.
     */
    @SuppressWarnings("unchecked")
    public void testNonStringKeysAndValuesAreSkippedWithWarning()
    {
        Properties p = new Properties();
        p.setProperty( "good.key", "good-value" );
        p.put( Integer.valueOf( 7 ), "value-under-non-string-key" );
        p.put( "key.with.non.string.value", Integer.valueOf( 9 ) );

        MultiPropertiesConfig mpc = MultiPropertiesConfig.fromProperties( "/mixed", p );

        assertEquals( "the well-formed entry should survive", "good-value", mpc.getProperty( "good.key" ) );
        assertNull( "a non-String value should not be exposed as a property",
                    mpc.getProperty( "key.with.non.string.value" ) );

        List<DelayedLogItem> items = mpc.getDelayedLogItems();
        assertTrue( "expected a WARNING about the non-String key, got: " + items,
                    containsWarningMentioning( items, "not a String" ) );
    }

    private static boolean containsWarningMentioning( List<DelayedLogItem> items, String fragment )
    {
        for ( DelayedLogItem item : items )
            if ( DelayedLogItem.Level.WARNING.equals( item.getLevel() )
                 && item.getText() != null && item.getText().contains( fragment ) )
                return true;
        return false;
    }
}
