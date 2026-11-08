package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  Behavior when no HOCON implementation is on the CLASSPATH.
 *
 *  typesafe-config is a compile-optional dependency and is present on the ordinary test
 *  classpath, so this branch is unreachable without a scenario ClassLoader that leaves
 *  the jar out -- which is what CfgScenario.open( name, false ) does.
 */
public final class HoconAbsentJUnitTestCase extends TestCase
{
    private static CfgScenario openWithoutHocon()
    { return CfgScenario.open( "hocon-absent", false ); }

    private static List<String> readPaths( CfgScenario s, Object mpc )
    { return Arrays.asList( s.getPropertiesResourcePaths( mpc ) ); }

    /** Sanity: the scenario really does lack typesafe-config. */
    public void testTypesafeConfigIsAbsentFromScenario()
    {
        CfgScenario s = openWithoutHocon();
        try
        {
            try
            {
                Class.forName( "com.typesafe.config.Config", false, s.classLoader() );
                fail( "typesafe-config should not be visible to this scenario" );
            }
            catch ( ClassNotFoundException expected )
            { /* as intended */ }
        }
        finally
        { s.closeQuietly(); }
    }

    /** A HOCON path contributes nothing, but the rest of the config still loads. */
    public void testHoconPathIsSkippedAndOtherSourcesStillLoad()
    {
        CfgScenario s = openWithoutHocon();
        try
        {
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:/reference", "/plain.properties" }, new ArrayList() );

            assertEquals( "the non-HOCON source must still be read",
                          "plain-value", s.getProperty( mpc, "plain.key" ) );
            assertNull( "no HOCON content should be present",
                        s.getProperty( mpc, "hocon.key" ) );
            assertEquals( Arrays.asList( "/plain.properties" ), readPaths( s, mpc ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  The skip is reported at FINE, naming the identifier the user actually wrote.
     *  (The FileNotFoundException raised internally mentions the hocon:-stripped path,
     *  but that message is discarded; firstInit rebuilds it from the original path.)
     */
    public void testSkipIsReportedAtFineNamingTheWrittenPath()
    {
        CfgScenario s = openWithoutHocon();
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/reference" }, new ArrayList() );
            List<String[]> items = s.getDelayedLogItems( mpc );

            assertTrue( "expected a FINE skip notice, got:" + CfgScenario.describe( items ),
                        CfgScenario.hasLogItem( items, "FINE", "could not be found. Skipping." ) );
            assertTrue( "the notice should name the path as written, got:" + CfgScenario.describe( items ),
                        CfgScenario.hasLogItem( items, "FINE", "hocon:/reference" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** The multi-element default path degrades the same way. */
    public void testDefaultMultiElementHoconPathDegradesCleanly()
    {
        CfgScenario s = openWithoutHocon();
        try
        {
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:/reference,/application,/", "/plain.properties" }, new ArrayList() );

            assertEquals( "plain-value", s.getProperty( mpc, "plain.key" ) );
            assertEquals( Arrays.asList( "/plain.properties" ), readPaths( s, mpc ) );

            List<String[]> items = s.getDelayedLogItems( mpc );
            assertTrue( "expected a FINE skip notice, got:" + CfgScenario.describe( items ),
                        CfgScenario.hasLogItem( items, "FINE", "could not be found. Skipping." ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  Nothing escalates any more, not even when the resource is really there.
     *
     *  <p>Through 0.7.0 this case produced a WARNING: the library chopped "hocon:" off the
     *  identifier, asked whether a resource sat at whatever was left, and if one did, said so
     *  loudly. Here <code>/reference.conf</code> genuinely exists, so that is what used to
     *  happen -- as the earlier version of this case asserted, describing the resource as one
     *  that "happens to sit" at the stripped path, which is the tell.</p>
     *
     *  <p>The check could not be made to work. A HOCON identifier is a comma-separated list
     *  whose elements may be URLs, may carry <code>#scope</code> suffixes, may need the three
     *  HOCON suffixes appended, and conventionally end with <code>/</code>, meaning System
     *  properties -- which is always satisfiable, so any honest check reports "might exist" for
     *  every realistic identifier. Rather than guess, 0.7.1 says the one thing it knows, once
     *  per identifier, at INFO: this will not be read.</p>
     */
    public void testAnExistingResourceAtTheStrippedPathNoLongerEscalates()
    {
        CfgScenario s = openWithoutHocon();
        try
        {
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:/reference.conf", "/plain.properties" }, new ArrayList() );

            assertEquals( "plain-value", s.getProperty( mpc, "plain.key" ) );

            List<String[]> items = s.getDelayedLogItems( mpc );
            assertFalse( "an absent optional dependency must not draw a WARNING, got:"
                             + CfgScenario.describe( items ),
                         CfgScenario.hasLogItem( items, "WARNING", "hocon:/reference.conf" ) );
            assertTrue( "the INFO notice should name the identifier, got:"
                            + CfgScenario.describe( items ),
                        CfgScenario.hasLogItem( items, "INFO", "hocon:/reference.conf" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  The notice is reported once per identifier, not once per read.
     *
     *  <p>This is the whole reason the message can afford to be INFO rather than FINE: c3p0 and
     *  mlog both put a <code>hocon:</code> element in their default source lists, so an ordinary
     *  deployment reaches this path on every configuration load. Before the dedup, one c3p0 test
     *  run emitted 216 of these and 115 stack traces.</p>
     */
    public void testTheUnavailabilityNoticeIsReportedOncePerIdentifier()
    {
        CfgScenario s = openWithoutHocon();
        try
        {
            String[] paths = new String[] { "hocon:/reference", "/plain.properties" };

            List<String[]> first  = s.getDelayedLogItems( s.asProvidedUncached( paths, new ArrayList() ) );
            List<String[]> second = s.getDelayedLogItems( s.asProvidedUncached( paths, new ArrayList() ) );

            assertTrue( "precondition: the first read must report it, got:" + CfgScenario.describe( first ),
                        CfgScenario.hasLogItem( first, "INFO", "hocon:/reference" ) );
            assertFalse( "a second read of the same identifier must not report it again, got:"
                             + CfgScenario.describe( second ),
                         CfgScenario.hasLogItem( second, "INFO", "hocon:/reference" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Layering among the surviving sources is unaffected. */
    public void testLayeringAmongSurvivingSourcesIsUnaffected()
    {
        CfgScenario s = openWithoutHocon();
        try
        {
            Object mpc = s.asProvidedUncached(
                new String[] { "/plain.properties", "hocon:/reference", "/" }, new ArrayList() );

            assertEquals( "plain-value", s.getProperty( mpc, "plain.key" ) );
            assertEquals( System.getProperty( "user.home" ), s.getProperty( mpc, "user.home" ) );
            assertEquals( Arrays.asList( "/plain.properties", "/" ), readPaths( s, mpc ) );
        }
        finally
        { s.closeQuietly(); }
    }
}
