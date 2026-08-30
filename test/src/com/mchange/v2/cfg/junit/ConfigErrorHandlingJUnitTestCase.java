package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  Bad configuration must never escape the MConfig facade as an exception. Every
 *  failure is absorbed into a DelayedLogItem, the offending path is dropped, and the
 *  remaining sources are read normally.
 */
public final class ConfigErrorHandlingJUnitTestCase extends TestCase
{
    private final static String GOOD = "/good.properties";

    private static List<String> readPaths( CfgScenario s, Object mpc )
    { return Arrays.asList( s.getPropertiesResourcePaths( mpc ) ); }

    /** A path naming no resource is skipped at FINE, and dropped from the path list. */
    public void testMissingResourceIsSkippedAtFine()
    {
        CfgScenario s = CfgScenario.open( "errors" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "/nonexistent.properties", GOOD }, new ArrayList() );

            assertEquals( "the good path should still be read", "yes", s.getProperty( mpc, "good" ) );
            assertEquals( "the missing path should be dropped", Arrays.asList( GOOD ), readPaths( s, mpc ) );

            List<String[]> items = s.getDelayedLogItems( mpc );
            assertTrue( "expected a FINE skip notice, got:" + CfgScenario.describe( items ),
                        CfgScenario.hasLogItem( items, "FINE", "could not be found. Skipping." ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  A path that is neither absolute nor a HOCON identifier raises
     *  IllegalArgumentException internally, which must be caught and reported.
     */
    public void testRelativePathIsReportedNotThrown()
    {
        CfgScenario s = CfgScenario.open( "errors" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "relative.properties", GOOD }, new ArrayList() );

            assertEquals( "yes", s.getProperty( mpc, "good" ) );
            assertEquals( Arrays.asList( GOOD ), readPaths( s, mpc ) );

            List<String[]> items = s.getDelayedLogItems( mpc );
            assertTrue( "expected a WARNING naming the bad identifier, got:" + CfgScenario.describe( items ),
                        CfgScenario.hasLogItem( items, "WARNING", "relative.properties" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  A bare "hocon:" used to reach normalizeHoconPathElement with an empty element and
     *  throw StringIndexOutOfBoundsException straight out of the facade. It must now be
     *  reported like any other malformed path.
     */
    public void testBareHoconPrefixIsReportedNotThrown()
    {
        CfgScenario s = CfgScenario.open( "errors" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:", GOOD }, new ArrayList() );

            assertEquals( "yes", s.getProperty( mpc, "good" ) );
            assertEquals( Arrays.asList( GOOD ), readPaths( s, mpc ) );
            assertFalse( "the malformed HOCON path should not have been read",
                         readPaths( s, mpc ).contains( "hocon:" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** A leading comma yields an empty first element; it must be dropped, not fatal. */
    public void testLeadingCommaInHoconPathIsTolerated()
    {
        CfgScenario s = CfgScenario.open( "hocon-basic" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:,/reference" }, new ArrayList() );
            assertEquals( "from-reference", s.getProperty( mpc, "hocon.key" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** A doubled comma likewise. */
    public void testDoubledCommaInHoconPathIsTolerated()
    {
        CfgScenario s = CfgScenario.open( "hocon-basic" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/reference,,/application" }, new ArrayList() );
            assertEquals( "from-application", s.getProperty( mpc, "hocon.key" ) );
            assertEquals( "yes", s.getProperty( mpc, "only.reference" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** A trailing comma likewise. */
    public void testTrailingCommaInHoconPathIsTolerated()
    {
        CfgScenario s = CfgScenario.open( "hocon-basic" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:/reference," }, new ArrayList() );
            assertEquals( "from-reference", s.getProperty( mpc, "hocon.key" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** A HOCON path consisting only of separators degenerates safely. */
    public void testHoconPathOfOnlyCommasIsReportedNotThrown()
    {
        CfgScenario s = CfgScenario.open( "errors" );
        try
        {
            Object mpc = s.asProvidedUncached( new String[] { "hocon:,,", GOOD }, new ArrayList() );
            assertEquals( "yes", s.getProperty( mpc, "good" ) );
            assertEquals( Arrays.asList( GOOD ), readPaths( s, mpc ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Several bad paths at once still leave the good ones intact and correctly ordered. */
    public void testMultipleBadPathsDoNotDisturbGoodOnes()
    {
        CfgScenario s = CfgScenario.open( "errors" );
        try
        {
            Object mpc = s.asProvidedUncached(
                new String[] { "hocon:", "relative.properties", "/missing.properties", GOOD }, new ArrayList() );

            assertEquals( "yes", s.getProperty( mpc, "good" ) );
            assertEquals( Arrays.asList( GOOD ), readPaths( s, mpc ) );
        }
        finally
        { s.closeQuietly(); }
    }
}
