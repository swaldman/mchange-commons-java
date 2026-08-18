package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 *  Verifies the CfgScenario harness itself. If these fail, nothing else in the
 *  cfg scenario suite can be trusted.
 */
public final class CfgScenarioSelfCheckJUnitTestCase extends TestCase
{
    private final static String SCENARIO = "harness-selfcheck";

    /** The scenario must define its own MConfig, not reuse the one on the test classpath. */
    public void testScenarioLoadsItsOwnCfgClasses()
    {
        CfgScenario s = CfgScenario.open( SCENARIO );
        try
        {
            Class<?> childMConfig = s.mconfigClass();
            assertTrue( "scenario MConfig should not be the test classpath's MConfig",
                        childMConfig != com.mchange.v2.cfg.MConfig.class );
            assertTrue( "scenario MConfig should still be named com.mchange.v2.cfg.MConfig, was " + childMConfig.getName(),
                        "com.mchange.v2.cfg.MConfig".equals( childMConfig.getName() ) );
            assertTrue( "scenario MConfig should have been defined by the scenario ClassLoader",
                        childMConfig.getClassLoader() == s.classLoader() );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Resources in the scenario directory are visible to the scenario's cfg classes. */
    public void testScenarioResourcesAreReadable()
    {
        CfgScenario s = CfgScenario.open( SCENARIO );
        try
        {
            Object mpc = s.asProvidedCached( new String[] { "/selfcheck.properties" } );
            assertEquals( "selfcheck-value", s.getProperty( mpc, "selfcheck.key" ) );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  Resources on the ordinary test classpath but NOT in the scenario must be invisible.
     *  This is the property the whole design rests on: without it, scenarios would be
     *  contaminated by src/test/resources (which contains /mchange-commons.properties,
     *  one of the hardcoded default paths).
     */
    public void testTestClasspathResourcesAreNotVisible()
    {
        CfgScenario s = CfgScenario.open( SCENARIO );
        try
        {
            // present on the real test classpath, absent from the scenario
            Object mpc = s.asProvidedCached( new String[] { "/com/mchange/v2/cfg/junit/a.properties" } );
            assertNull( "test-classpath resource leaked into the scenario",
                        s.getProperty( mpc, "user.home" ) );
            String[] paths = s.getPropertiesResourcePaths( mpc );
            assertEquals( "unreadable path should be dropped, got " + Arrays.toString( paths ),
                          0, paths.length );
        }
        finally
        { s.closeQuietly(); }
    }

    /** Each scenario gets independent copies of the package's process-wide statics. */
    public void testScenariosAreMutuallyIsolated()
    {
        CfgScenario a = CfgScenario.open( SCENARIO );
        CfgScenario b = CfgScenario.open( SCENARIO );
        try
        {
            assertTrue( "two scenarios should not share a MConfig class",
                        a.mconfigClass() != b.mconfigClass() );

            String[] paths = new String[] { "/selfcheck.properties" };
            Object fromA1 = a.asProvidedCached( paths );
            Object fromA2 = a.asProvidedCached( paths );
            Object fromB  = b.asProvidedCached( paths );

            assertSame( "within one scenario the cache should return the same instance", fromA1, fromA2 );
            assertTrue( "across scenarios the caches must be independent", fromA1 != fromB );
        }
        finally
        {
            a.closeQuietly();
            b.closeQuietly();
        }
    }

    /** The JDK must remain reachable through the platform parent loader. */
    public void testSystemPropertiesSourceWorksInScenario()
    {
        CfgScenario s = CfgScenario.open( SCENARIO );
        try
        {
            Object mpc = s.asProvidedCached( new String[] { "/" } );
            assertEquals( System.getProperty( "user.home" ), s.getProperty( mpc, "user.home" ) );
        }
        finally
        { s.closeQuietly(); }
    }
}
