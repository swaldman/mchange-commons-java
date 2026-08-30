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
     *  The one case that escalates: if a real resource happens to sit at the path you get
     *  by chopping "hocon:" off the identifier, the library reports a WARNING carrying an
     *  exception instead of a quiet FINE skip. Here /reference.conf genuinely exists.
     */
    public void testExistingResourceAtStrippedPathEscalatesToWarning()
    {
        CfgScenario s = openWithoutHocon();
        try
        {
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:/reference.conf", "/plain.properties" }, new ArrayList() );

            assertEquals( "plain-value", s.getProperty( mpc, "plain.key" ) );

            List<String[]> items = s.getDelayedLogItems( mpc );
            assertTrue( "expected a WARNING for the existing-but-unparseable resource, got:"
                            + CfgScenario.describe( items ),
                        CfgScenario.hasLogItem( items, "WARNING", "hocon:/reference.conf" ) );
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
