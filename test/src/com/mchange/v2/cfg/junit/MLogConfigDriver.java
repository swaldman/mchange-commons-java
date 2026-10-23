package com.mchange.v2.cfg.junit;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 *  Reflective access to one scenario's MLogConfig and SealedSystemProperties.
 *
 *  <p>Everything is reached with getDeclaredMethod(..) and setAccessible(true) rather than
 *  getMethod(..), because MLogConfig's API is package-private on purpose: clients are meant to
 *  arrive through MLog.refreshConfig(..), which both guarantees an initialized MLog and spawns
 *  the MLog-Init-Reporter that drains the delayed log items. A test driving MLogConfig directly
 *  is deliberately doing what clients may not, so it has to reach past the modifier that stops
 *  them.</p>
 */
final class MLogConfigDriver
{
    private final ClassLoader cl;
    private final Class<?>    mlogConfigClass;
    private final Class<?>    mpcClass;
    private final Class<?>    sealedClass;
    private final Class<?>    mloggerClass;

    MLogConfigDriver( CfgScenario s )
    {
        this.cl              = s.classLoader();
        this.mlogConfigClass = load( "com.mchange.v2.log.MLogConfig" );
        this.mpcClass        = load( "com.mchange.v2.cfg.MultiPropertiesConfig" );
        this.sealedClass     = load( "com.mchange.v2.cfg.SealedSystemProperties" );
        this.mloggerClass    = load( "com.mchange.v2.log.MLogger" );
    }

    private Class<?> load( String cn )
    {
        try
        { return Class.forName( cn, true, cl ); }
        catch ( ClassNotFoundException e )
        { throw new RuntimeException( "Scenario ClassLoader could not load " + cn, e ); }
    }

    private Method reach( String name, Class<?>... parameterTypes )
    {
        try
        {
            Method m = mlogConfigClass.getDeclaredMethod( name, parameterTypes );
            m.setAccessible( true );
            return m;
        }
        catch ( Exception e )
        { throw new RuntimeException( "Scenario's MLogConfig has no reachable " + name + "(..)", e ); }
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
            reach( "refresh", Array.newInstance( mpcClass, 0 ).getClass(), String.class )
                .invoke( null, new Object[] { arr, description } );
        }
        catch ( Exception e )
        { throw new RuntimeException( "MLogConfig.refresh(..) failed in the scenario", e ); }
    }

    /**
     *  Reads through getPropertyOnlyIfAvailable(..) rather than getProperty(..): the latter
     *  routes to ensureLoad(), so a read could itself provoke a refresh, and a test that reads
     *  after refreshing would then be measuring something it caused.
     */
    String getProperty( String key )
    {
        try
        { return (String) reach( "getPropertyOnlyIfAvailable", String.class ).invoke( null, key ); }
        catch ( Exception e )
        { throw new RuntimeException( "MLogConfig.getPropertyOnlyIfAvailable(..) failed in the scenario", e ); }
    }

    /**
     *  Initializes this scenario's MLog, for real, which performs MLogConfig's first load as a
     *  side effect and marks the scenario initialized the way production does.
     *
     *  <p>This is how a test reaches the post-initialization regime. Faking it by installing a
     *  classLogger alone does not work: the predicate is not a flag but a claim that MLog
     *  functions, and the freeze check immediately acts on it by sealing System properties --
     *  a read that itself logs. With MLog actually down that recurses into
     *  SealedSystemProperties.get() before its snapshot is assigned, finds _mlog null, and the
     *  NPE escapes to MLog's initializer, which falls back to System.err.</p>
     */
    void initializeMLog()
    {
        try { Class.forName( "com.mchange.v2.log.MLog", true, cl ); }
        catch ( Throwable t )
        { throw new RuntimeException( "Scenario's MLog would not initialize", t ); }
    }

    /**
     *  Swaps MLogConfig's cached logger for one that records. Call AFTER initializeMLog(): this
     *  is for reading what MLogConfig says, not for pretending MLog is up.
     *
     *  <p>Safe to leave installed, because markMLogReinitialized() -- which would replace it --
     *  runs from MLog.refreshConfig(..) after MLogConfig.refresh(..) returns, and these tests
     *  call MLogConfig.refresh(..) directly.</p>
     *  @return one {level, text, thrownClassName-or-null} per record, in order.
     */
    List<String[]> installRecordingClassLogger()
    {
        final List<String[]> recorded = new ArrayList<String[]>();

        Object logger = Proxy.newProxyInstance( cl, new Class[] { mloggerClass }, new InvocationHandler()
        {
            @Override
            public Object invoke( Object proxy, Method method, Object[] args )
            {
                String name = method.getName();

                if ( "log".equals( name ) && args != null && args.length >= 2 )
                {
                    Throwable t = ( args.length > 2 && args[2] instanceof Throwable ) ? (Throwable) args[2] : null;
                    recorded.add( new String[] { String.valueOf( args[0] ),
                                                 String.valueOf( args[1] ),
                                                 t == null ? null : t.getClass().getName() } );
                }

                if ( "hashCode".equals( name ) ) return Integer.valueOf( System.identityHashCode( proxy ) );
                if ( "equals".equals( name ) )   return Boolean.valueOf( proxy == args[0] );
                if ( "toString".equals( name ) ) return "recording MLogger proxy";

                Class<?> rt = method.getReturnType();
                if ( rt == boolean.class ) return Boolean.TRUE;   // isLoggable: record everything
                if ( rt == String.class )  return "";
                if ( rt.isPrimitive() && rt != void.class ) return Integer.valueOf( 0 );
                return null;
            }
        } );

        try
        {
            Field f = mlogConfigClass.getDeclaredField( "classLogger" );
            f.setAccessible( true );
            f.set( null, logger );
        }
        catch ( Exception e )
        { throw new RuntimeException( "Could not install a stub classLogger in the scenario", e ); }

        return recorded;
    }

    /** Records at WARNING or above whose text contains the fragment. */
    static List<String[]> warningsContaining( List<String[]> recorded, String fragment )
    {
        List<String[]> out = new ArrayList<String[]>();
        for ( String[] r : recorded )
            // MLevel renders as "com.mchange.v2.log.MLevel[WARNING]", so match within rather
            // than against: the level matters (a refusal visible only at FINE is no refusal),
            // but its spelling is MLevel's business.
            if ( ( r[0].contains( "WARNING" ) || r[0].contains( "SEVERE" ) ) && r[1].indexOf( fragment ) >= 0 )
                out.add( r );
        return out;
    }

    static String describe( List<String[]> recorded )
    {
        if ( recorded.isEmpty() ) return "(nothing logged)";
        StringBuilder sb = new StringBuilder();
        for ( String[] r : recorded )
            sb.append( '[' ).append( r[0] ).append( "] " ).append( r[1] )
              .append( r[2] == null ? "" : " (+" + r[2] + ")" ).append( '\n' );
        return sb.toString();
    }
}
