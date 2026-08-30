package com.mchange.v2.cfg.junit;

import junit.framework.*;
import com.mchange.v2.cfg.*;

/**
 *  The original cfg test case, retained and extended.
 *
 *  The testNoSystemConfig / testSystemShadows / testSystemShadowed trio predates the
 *  facade reorganization and still exercises the deprecated MConfig.readConfig shim,
 *  which is worth keeping as compatibility coverage. Each is paired with an equivalent
 *  written against the supported MConfig.AsProvided API.
 *
 *  These run on the ordinary test classpath rather than through CfgScenario, because
 *  they depend only on named resources, not on the presence or absence of the magic
 *  resource-path files.
 */
public final class MConfigJUnitTestCase extends TestCase
{
    final static String RP_A = "/com/mchange/v2/cfg/junit/a.properties";
    final static String RP_B = "/com/mchange/v2/cfg/junit/b.properties";

    // ------------------------------------------------ deprecated API (compat)

    @SuppressWarnings("deprecation")
    public void testNoSystemConfig()
    {
	MultiPropertiesConfig mpc = MConfig.readConfig(new String[] {RP_A, RP_B});
	assertTrue( "/b/home".equals( mpc.getProperty( "user.home" ) ) );
    }

    @SuppressWarnings("deprecation")
    public void testSystemShadows()
    {
	MultiPropertiesConfig mpc = MConfig.readConfig(new String[] {RP_A, RP_B, "/"});
	assertTrue( (! "/b/home".equals( mpc.getProperty( "user.home" ) ) ) && 
		    (! "/a/home".equals( mpc.getProperty( "user.home" ) ) ) );
    }

    @SuppressWarnings("deprecation")
    public void testSystemShadowed()
    {
	MultiPropertiesConfig mpc = MConfig.readConfig(new String[] {RP_A, "/", RP_B});
	assertTrue( "/b/home".equals( mpc.getProperty( "user.home" ) ) );
    }

    // ---------------------------------------------------- supported API

    public void testNoSystemConfigAsProvided()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readCachedClassloaderResourceConfig( new String[] {RP_A, RP_B} );
        assertEquals( "/b/home", mpc.getProperty( "user.home" ) );
    }

    public void testSystemShadowsAsProvided()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readCachedClassloaderResourceConfig( new String[] {RP_A, RP_B, "/"} );
        assertEquals( System.getProperty( "user.home" ), mpc.getProperty( "user.home" ) );
    }

    public void testSystemShadowedAsProvided()
    {
        MultiPropertiesConfig mpc =
            MConfig.AsProvided.readCachedClassloaderResourceConfig( new String[] {RP_A, "/", RP_B} );
        assertEquals( "/b/home", mpc.getProperty( "user.home" ) );
    }

    /** AsProvided also offers uncached reads, as the class documentation promises. */
    public void testAsProvidedUncachedReadsWork()
    {
        MultiPropertiesConfig first =
            MConfig.AsProvided.readUncachedClassloaderResourceConfig( new String[] {RP_A, RP_B} );
        MultiPropertiesConfig second =
            MConfig.AsProvided.readUncachedClassloaderResourceConfig( new String[] {RP_A, RP_B} );

        assertEquals( "/b/home", first.getProperty( "user.home" ) );
        assertTrue( "uncached reads should not be shared", first != second );
    }

    // -------------------------------------- deprecated shims delegate correctly

    /** MConfig.readConfig is exactly MConfig.AsProvided.readCachedClassloaderResourceConfig. */
    @SuppressWarnings("deprecation")
    public void testReadConfigDelegatesToAsProvidedCached()
    {
        String[] paths = new String[] { RP_A, RP_B };
        assertSame( MConfig.AsProvided.readCachedClassloaderResourceConfig( paths ),
                    MConfig.readConfig( paths ) );
    }

    /** MConfig.readVmConfig() is exactly the traditional cached read. */
    @SuppressWarnings("deprecation")
    public void testReadVmConfigDelegatesToTraditionalCached()
    {
        assertSame( MConfig.WithTraditionalDefaultSources.readCachedClassloaderResourceConfig(),
                    MConfig.readVmConfig() );
    }
}
