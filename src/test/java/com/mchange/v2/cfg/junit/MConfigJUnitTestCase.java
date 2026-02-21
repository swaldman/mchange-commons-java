package com.mchange.v2.cfg.junit;

import junit.framework.*;
import com.mchange.v2.cfg.*;

public final class MConfigJUnitTestCase extends TestCase
{
    //private final static com.mchange.v2.log.MLogger logger = com.mchange.v2.log.MLog.getLogger( com.mchange.v2.log.MLog.class );

    final static String RP_A = "/com/mchange/v2/cfg/junit/a.properties";
    final static String RP_B = "/com/mchange/v2/cfg/junit/b.properties";

    public void testNoSystemConfig()
    {
	MultiPropertiesConfig mpc = MConfig.readConfig(new String[] {RP_A, RP_B});
	//System.err.println(mpc.getProperty( "user.home" ));
	assertTrue( "/b/home".equals( mpc.getProperty( "user.home" ) ) );
    }

    public void testSystemShadows()
    {
	MultiPropertiesConfig mpc = MConfig.readConfig(new String[] {RP_A, RP_B, "/"});
	//System.err.println(mpc.getProperty( "user.home" ));
	assertTrue( (! "/b/home".equals( mpc.getProperty( "user.home" ) ) ) && 
		    (! "/a/home".equals( mpc.getProperty( "user.home" ) ) ) );
    }

    public void testSystemShadowed()
    {
	MultiPropertiesConfig mpc = MConfig.readConfig(new String[] {RP_A, "/", RP_B});
	//System.err.println(mpc.getProperty( "user.home" ));
	assertTrue( "/b/home".equals( mpc.getProperty( "user.home" ) ) );
    }
}
