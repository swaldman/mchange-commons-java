package com.mchange.v2.cfg.junit;

import junit.framework.TestCase;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *  What MLogConfig.logDelayedItems() actually emits.
 *
 *  <p>MLog collects DelayedLogItems from its own configuration read into a list it keeps
 *  (bootstrapLogItems), and the config it builds keeps a copy of the same items -- except that
 *  a config's copy is STRIPPED of Throwables, so that a long-lived config cannot pin a
 *  ClassLoader through an exception's stack trace. Two lists, same messages, one of them
 *  stripped.</p>
 *
 *  <p>That is the whole problem. DelayedLogItem.equals includes the exception, so an item and
 *  its stripped twin are unequal, and a HashSet cannot collapse them: every message that
 *  carried a Throwable got logged twice. Three successive fixes each closed one path and left
 *  another open, because nothing here was covered:</p>
 *
 *  <ul>
 *    <li>uniting the two lists and deduplicating on DelayedLogItem.equals -- duplicates at
 *        ordinary bootstrap;</li>
 *    <li>dropping the config's list entirely -- an override's own items stopped being logged
 *        at all, since with overrides the config is a COMBINE and holds items bootstrapLogItems
 *        never saw;</li>
 *    <li>adding back only each override's own items -- an override built through MConfig is
 *        itself stripped, so one that happens to repeat a bootstrap message duplicated again.</li>
 *  </ul>
 *
 *  <p>Only deduplicating on (level, text) closes all three, which is why these tests assert on
 *  that identity rather than on item equality.</p>
 *
 *  <h3>Why it is driven this way</h3>
 *
 *  <p>Through a CfgScenario, because MLogConfig's state is static and one-shot: the scenario
 *  ClassLoader gives each case its own copy. Not by initializing MLog, because MLog dumps these
 *  items from the asynchronous MLog-Init-Reporter thread, and whichever of that thread and the
 *  test called logDelayedItems() first would drain them -- a race, not a test. MLogConfig.refresh
 *  and logDelayedItems are called directly instead, and MLogger is an interface, so a Proxy
 *  standing in for the logger captures exactly the (level, text, throwable) triples emitted,
 *  with no logging backend in the way.</p>
 */
public final class MLogDelayedItemLoggingJUnitTestCase extends TestCase
{
    /**
     *  A scenario with no resource-path text files, so the read falls back to the hardcoded
     *  defaults -- which include a hocon: path. Opened WITHOUT typesafe-config, so probing for
     *  the HOCON library throws ClassNotFoundException and the resulting item carries a
     *  Throwable. That item is the one that used to double-log; a scenario whose items were all
     *  Throwable-free could not detect any of this.
     */
    private final static String SCENARIO = "no-pathfiles";

    private final static String HOCON_ABSENT_ITEM = "Class not found while testing for HOCON lib";
    private final static String OVERRIDE_RESOURCE = "/no-such-override-resource.properties";

    /** An identifier whose read repeats a message MLog's own bootstrap read already produced. */
    private final static String ECHOES_BOOTSTRAP  = "hocon:/reference,/application,/";

    // ------------------------------------------------------------- the tests

    /** The plain case: nothing may be logged twice, and the stripped twin is what would double it. */
    public void testBootstrapItemsAreNotLoggedTwice()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            Driver d = new Driver( s );
            d.refresh( null, null );
            List<String[]> logged = d.logDelayedItems();

            assertTrue( "nothing was logged, so this test would pass vacuously",
                        contains( logged, HOCON_ABSENT_ITEM ) );
            assertNoDuplicates( logged );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  With overrides the config is a COMBINE, whose items include each override's own. Those
     *  are not in bootstrapLogItems by any other route, so dropping the config's list silently
     *  stopped reporting whatever an override had to say.
     */
    public void testAnOverridesOwnItemsAreLogged()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            Driver d = new Driver( s );
            d.refresh( new Object[] { d.readAsProvided( OVERRIDE_RESOURCE ) }, "test override" );
            List<String[]> logged = d.logDelayedItems();

            assertTrue( "the override's own item was never logged: " + describe( logged ),
                        contains( logged, OVERRIDE_RESOURCE ) );
            assertNoDuplicates( logged );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  The case that defeats curating the inputs. An override built through MConfig carries
     *  STRIPPED items; when one of them repeats a bootstrap message, the unstripped original
     *  and the stripped twin are unequal and both get through -- unless dedup ignores the
     *  exception. No amount of choosing what to add to the list can prevent this, because the
     *  application chooses what its overrides contain.
     */
    public void testAnOverrideEchoingBootstrapDoesNotDuplicate()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            Driver d = new Driver( s );
            d.refresh( new Object[] { d.readAsProvided( ECHOES_BOOTSTRAP ) }, "echoing override" );
            List<String[]> logged = d.logDelayedItems();

            assertTrue( "the echoed message never appeared, so this test would pass vacuously",
                        contains( logged, HOCON_ABSENT_ITEM ) );
            assertNoDuplicates( logged );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  Collapsing a pair must keep the informative half. Preferring the stripped copy would
     *  trade a duplicate for a silently less useful record -- the same message, minus the
     *  exception that explains it.
     */
    public void testTheSurvivingCopyKeepsItsThrowable()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            Driver d = new Driver( s );
            d.refresh( new Object[] { d.readAsProvided( ECHOES_BOOTSTRAP ) }, "echoing override" );
            List<String[]> logged = d.logDelayedItems();

            List<String[]> matches = new ArrayList<String[]>();
            for ( String[] rec : logged )
                if ( rec[1] != null && rec[1].contains( HOCON_ABSENT_ITEM ) ) matches.add( rec );

            assertEquals( "expected the echoed message exactly once, got: " + describe( logged ),
                          1, matches.size() );
            assertNotNull( "the surviving copy lost its Throwable; the stripped twin won the dedup",
                           matches.get( 0 )[2] );
        }
        finally
        { s.closeQuietly(); }
    }

    /**
     *  Logged once, then released. The items hold Throwables, and a Throwable's stack trace
     *  holds Classes, and those hold a ClassLoader -- so the list is dropped after it is
     *  drained rather than retained for the life of the VM.
     */
    public void testItemsAreLoggedOnlyOnce()
    {
        CfgScenario s = CfgScenario.open( SCENARIO, false );
        try
        {
            Driver d = new Driver( s );
            d.refresh( null, null );

            List<String[]> first  = d.logDelayedItems();
            List<String[]> second = d.logDelayedItems();

            assertTrue( "nothing was logged the first time, so this test would pass vacuously",
                        first.size() > 0 );
            assertEquals( "a second drain should log nothing, got: " + describe( second ),
                          0, second.size() );
        }
        finally
        { s.closeQuietly(); }
    }

    // ------------------------------------------------------------- assertions

    /** Identity is (level, text): a stripped item and its original are the same message. */
    private static void assertNoDuplicates( List<String[]> logged )
    {
        Map<String,Integer> counts = new LinkedHashMap<String,Integer>();
        for ( String[] rec : logged )
        {
            String key = rec[0] + " | " + rec[1];
            Integer n = counts.get( key );
            counts.put( key, n == null ? 1 : n + 1 );
        }
        StringBuffer sb = new StringBuffer();
        for ( Map.Entry<String,Integer> e : counts.entrySet() )
            if ( e.getValue() > 1 )
                sb.append( "\n    x" ).append( e.getValue() ).append( "  " ).append( e.getKey() );

        if ( sb.length() > 0 )
            fail( "these messages were logged more than once:" + sb );
    }

    private static boolean contains( List<String[]> logged, String fragment )
    {
        for ( String[] rec : logged )
            if ( rec[1] != null && rec[1].contains( fragment ) ) return true;
        return false;
    }

    private static String describe( List<String[]> logged )
    {
        StringBuffer sb = new StringBuffer();
        for ( String[] rec : logged )
            sb.append( "\n    [" ).append( rec[0] ).append( "] " ).append( rec[1] )
              .append( rec[2] == null ? "" : "   (" + rec[2] + ")" );
        return sb.length() == 0 ? "(nothing logged)" : sb.toString();
    }

    // ------------------------------------------------------------- the driver

    /**
     *  Reflective access to one scenario's MLogConfig, plus a Proxy MLogger that records what
     *  reaches it. Everything is loaded through the scenario ClassLoader: its MLogConfig, its
     *  MLogger and its MultiPropertiesConfig are not the ones on the ordinary test classpath.
     */
    private static final class Driver
    {
        private final ClassLoader cl;
        private final Class<?>    mlogConfigClass;
        private final Class<?>    mloggerClass;
        private final Class<?>    mpcClass;

        Driver( CfgScenario s )
        {
            this.cl              = s.classLoader();
            this.mlogConfigClass = load( "com.mchange.v2.log.MLogConfig" );
            this.mloggerClass    = load( "com.mchange.v2.log.MLogger" );
            this.mpcClass        = load( "com.mchange.v2.cfg.MultiPropertiesConfig" );
        }

        private Class<?> load( String cn )
        {
            try
            { return Class.forName( cn, true, cl ); }
            catch ( ClassNotFoundException e )
            { throw new RuntimeException( "Scenario ClassLoader could not load " + cn, e ); }
        }

        /** A MultiPropertiesConfig, read through the scenario's own MConfig, to use as an override. */
        Object readAsProvided( String identifier )
        {
            try
            {
                Class<?> asProvided = load( "com.mchange.v2.cfg.MConfig$AsProvided" );
                Method read = asProvided.getMethod( "readUncachedClassloaderResourceConfig",
                                                    String[].class, String[].class, List.class );
                return read.invoke( null, new Object[] { new String[0],
                                                         new String[] { identifier },
                                                         new ArrayList() } );
            }
            catch ( Exception e )
            { throw new RuntimeException( "Could not build an override config for '" + identifier + "'", e ); }
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
                Method m = mlogConfigClass.getMethod( "refresh", Array.newInstance( mpcClass, 0 ).getClass(), String.class );
                m.invoke( null, new Object[] { arr, description } );
            }
            catch ( Exception e )
            { throw new RuntimeException( "MLogConfig.refresh(..) failed in the scenario", e ); }
        }

        /** @return one {level, text, thrownClassName-or-null} per record the logger received. */
        List<String[]> logDelayedItems()
        {
            final List<String[]> recorded = new ArrayList<String[]>();

            Object logger = Proxy.newProxyInstance( cl, new Class[] { mloggerClass }, new InvocationHandler()
            {
                public Object invoke( Object proxy, Method method, Object[] args )
                {
                    String name = method.getName();

                    if ( "log".equals( name ) && args != null && args.length == 3 && args[2] instanceof Throwable )
                        recorded.add( new String[] { String.valueOf( args[0] ),
                                                     (String) args[1],
                                                     args[2].getClass().getName() } );
                    else if ( "log".equals( name ) && args != null && args.length == 3 )
                        recorded.add( new String[] { String.valueOf( args[0] ), (String) args[1], null } );
                    else if ( "log".equals( name ) && args != null && args.length == 2 )
                        recorded.add( new String[] { String.valueOf( args[0] ), (String) args[1], null } );

                    // Object methods and any incidental interrogation the dump might do
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
                Method m = mlogConfigClass.getMethod( "logDelayedItems", mloggerClass );
                m.invoke( null, new Object[] { logger } );
            }
            catch ( Exception e )
            { throw new RuntimeException( "MLogConfig.logDelayedItems(..) failed in the scenario", e ); }

            return recorded;
        }
    }
}
