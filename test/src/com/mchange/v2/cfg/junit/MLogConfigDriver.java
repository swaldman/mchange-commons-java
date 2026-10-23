package com.mchange.v2.cfg.junit;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Properties;

/** Reflective access to one scenario's MLogConfig and SealedSystemProperties. */
final class MLogConfigDriver
{
    private final ClassLoader cl;
    private final Class<?>    mlogConfigClass;
    private final Class<?>    mpcClass;
    private final Class<?>    sealedClass;

MLogConfigDriver( CfgScenario s )
    {
        this.cl              = s.classLoader();
        this.mlogConfigClass = load( "com.mchange.v2.log.MLogConfig" );
        this.mpcClass        = load( "com.mchange.v2.cfg.MultiPropertiesConfig" );
        this.sealedClass     = load( "com.mchange.v2.cfg.SealedSystemProperties" );
    }

    private Class<?> load( String cn )
    {
        try
        { return Class.forName( cn, true, cl ); }
        catch ( ClassNotFoundException e )
        { throw new RuntimeException( "Scenario ClassLoader could not load " + cn, e ); }
    }

    boolean isSealed()
    {
        try
        { return ((Boolean) sealedClass.getMethod( "isSealed" ).invoke( null )).booleanValue(); }
        catch ( Exception e )
        { throw new RuntimeException( "SealedSystemProperties.isSealed() failed in the scenario", e ); }
    }

    /** A one-key override, built through the scenario's own MultiPropertiesConfig. */
    Object override( String key, String value )
    {
        try
        {
            Properties p = new Properties();
            p.setProperty( key, value );
            Method m = mpcClass.getMethod( "fromProperties", Properties.class );
            return m.invoke( null, new Object[] { p } );
        }
        catch ( Exception e )
        { throw new RuntimeException( "Could not build an override config", e ); }
    }

    void refresh( Object[] overrides, String description )
    {
        try
        {
            Object arr = null;
            if ( overrides != null )
            {
                arr = Array.newInstance( mpcClass, overrides.length );
                for ( int i = 0; i < overrides.length; ++i ) Array.set( arr, i, overrides[i] );
            }
            Method m = mlogConfigClass.getMethod( "refresh",
                                                  Array.newInstance( mpcClass, 0 ).getClass(),
                                                  String.class );
            m.invoke( null, new Object[] { arr, description } );
        }
        catch ( Exception e )
        { throw new RuntimeException( "MLogConfig.refresh(..) failed in the scenario", e ); }
    }

    String getProperty( String key )
    {
        try
        { return (String) mlogConfigClass.getMethod( "getProperty", String.class ).invoke( null, key ); }
        catch ( Exception e )
        { throw new RuntimeException( "MLogConfig.getProperty(..) failed in the scenario", e ); }
    }
}
