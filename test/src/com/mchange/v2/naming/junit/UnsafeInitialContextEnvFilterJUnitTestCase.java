package com.mchange.v2.naming.junit;

import java.util.Hashtable;
import java.util.Properties;

import junit.framework.TestCase;

import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.cfg.SecurityRatchetTestSupport;
import com.mchange.v2.naming.AlwaysForbidUnsafeInitialContextEnvFilter;
import com.mchange.v2.naming.AlwaysReplaceWithDefaultUnsafeInitialContextEnvFilter;
import com.mchange.v2.naming.ForbiddenInitialContextException;
import com.mchange.v2.naming.ReferenceableUtils;
import com.mchange.v2.naming.SecurityConfigKey;
import com.mchange.v2.naming.UnsafeInitialContextEnvFilter;
import com.mchange.v2.reflect.ByNameInstantiationUtils;

/**
 *  An InitialContext environment that arrives on a deserialized or dereferenced object can
 *  name the context factory to instantiate and the server to fetch from, so it is the JNDI
 *  injection vector in miniature. c3p0 and ReferenceIndirector used to settle this with a
 *  single boolean: refuse every such environment, or accept it whole. An
 *  UnsafeInitialContextEnvFilter is what sits between those, and it is named by the deployment
 *  -- never by the object carrying the environment, which is the party we do not trust.
 *
 *  <p>These tests cover the filter mechanism itself: how the class is resolved, and what the
 *  two shipped implementations do. The call site that consumes it is covered in
 *  {@link ReferenceIndirectorJUnitTestCase}.</p>
 */
public class UnsafeInitialContextEnvFilterJUnitTestCase extends TestCase
{
    /**
     *  A bespoke filter of the kind a deployment is expected to write: it keeps the entries it
     *  recognizes as safe and drops the rest, rather than accepting or refusing wholesale.
     */
    public static class DropFactoryKeysFilter implements UnsafeInitialContextEnvFilter
    {
        @Override
        public Hashtable<?,?> safeEnv( Hashtable<?,?> env, Class<?> materializingClass, PropertiesConfig pcfg )
        {
            Hashtable<Object,Object> out = new Hashtable<Object,Object>();
            for ( Object k : env.keySet() )
                if ( !String.valueOf( k ).startsWith( "java.naming.factory" ) )
                    out.put( k, env.get( k ) );
            return out;
        }
    }

    /** Records what it was handed, so a caller's arguments can be asserted on. */
    public static class RecordingFilter implements UnsafeInitialContextEnvFilter
    {
        public static volatile Hashtable<?,?> lastEnv;
        public static volatile Class<?>       lastMaterializingClass;

        @Override
        public Hashtable<?,?> safeEnv( Hashtable<?,?> env, Class<?> materializingClass, PropertiesConfig pcfg )
        {
            lastEnv = env;
            lastMaterializingClass = materializingClass;
            return env;
        }
    }

    private static PropertiesConfig filterCfg( Class<?> filterClass )
    {
        Properties p = new Properties();
        p.setProperty( SecurityConfigKey.UNSAFE_INITIAL_CONTEXT_ENV_FILTER_CLASS_NAME, filterClass.getName() );
        return MultiPropertiesConfig.fromProperties( "/notional-test-resource", p );
    }

    private static Hashtable<String,String> hostileEnv()
    {
        Hashtable<String,String> env = new Hashtable<String,String>();
        env.put( "java.naming.factory.initial", "com.example.EvilContextFactory" );
        env.put( "java.naming.provider.url",    "ldap://evil.example.com:1389/payload" );
        env.put( "java.naming.batchsize",       "10" );
        return env;
    }

    /**
     *  Filters are resolved once per class name and the instance is kept, since the interface
     *  contracts them to be stateless and sharable. That cache is static and lives for the
     *  life of the JVM, so without clearing it a test can be satisfied by an instance some
     *  earlier test created -- and any test about how resolution behaves would quietly stop
     *  exercising resolution at all. Found exactly that way: a mutation that re-gated
     *  instantiation broke nothing, because nothing was being instantiated any more.
     */
    private static void clearFilterCache() throws Exception
    {
        java.lang.reflect.Field f = ReferenceableUtils.class.getDeclaredField(
            "unsafeInitialContextEnvFilterClassNameToInstance" );
        f.setAccessible( true );
        ((java.util.Map<?,?>) f.get( null )).clear();
    }

    @Override
    protected void setUp() throws Exception
    {
        resetEverythingThisTouches();
    }

    @Override
    protected void tearDown() throws Exception
    {
        resetEverythingThisTouches();
    }

    /**
     *  Both classes, deliberately. The gates this exercises are split across them --
     *  ReferenceableUtils holds the filter class name, ByNameInstantiationUtils holds
     *  enforceWhitelist -- and resetting only the first leaves the second latched at whatever
     *  an earlier test left it. That is a quiet failure: the test still passes, having
     *  exercised nothing.
     */
    private static void resetEverythingThisTouches() throws Exception
    {
        SecurityRatchetTestSupport.resetAll( ReferenceableUtils.class );
        SecurityRatchetTestSupport.resetAll( ByNameInstantiationUtils.class );
        clearFilterCache();
    }

    // ==================== which filter is in force ====================

    /**
     *  Unconfigured, the policy is to refuse. That preserves the behaviour that existed before
     *  filters: a non-default environment of untrusted provenance is simply not used.
     */
    public void testTheDefaultFilterRefusesEverything() throws Exception
    {
        UnsafeInitialContextEnvFilter filter = ReferenceableUtils.getUnsafeInitialContextEnvFilter( null );

        assertEquals( AlwaysForbidUnsafeInitialContextEnvFilter.class, filter.getClass() );
        try
        {
            filter.safeEnv( hostileEnv(), getClass(), null );
            fail( "The default filter must refuse." );
        }
        catch ( ForbiddenInitialContextException e ) { /* expected */ }
    }

    public void testAConfiguredFilterIsUsedInsteadOfTheDefault() throws Exception
    {
        UnsafeInitialContextEnvFilter filter =
            ReferenceableUtils.getUnsafeInitialContextEnvFilter( filterCfg( DropFactoryKeysFilter.class ) );

        assertEquals( DropFactoryKeysFilter.class, filter.getClass() );
    }

    /**
     *  Resolution must survive whitelist enforcement. The filter class name comes from
     *  deployment configuration rather than from the untrusted object, which is the documented
     *  case for instantiating ungated -- gating it would ask the deployer to authorize a choice
     *  only the deployer can make, and would leave the shipped default unusable in exactly the
     *  deployments that hardened themselves.
     */
    public void testFilterResolutionIsNotSubjectToTheByNameWhitelist() throws Exception
    {
        String key = "com.mchange.v2.reflect.byNameInstantiation.enforceWhitelist";
        String saved = System.getProperty( key );
        try
        {
            System.setProperty( key, "true" );
            resetEverythingThisTouches(); // so the seal, and enforceWhitelist, pick it up

            assertEquals( "The shipped default must resolve even under enforcement.",
                          AlwaysForbidUnsafeInitialContextEnvFilter.class,
                          ReferenceableUtils.getUnsafeInitialContextEnvFilter( null ).getClass() );
            assertEquals( "and so must a filter the deployment named, which is equally its own choice.",
                          DropFactoryKeysFilter.class,
                          ReferenceableUtils.getUnsafeInitialContextEnvFilter( filterCfg( DropFactoryKeysFilter.class ) ).getClass() );
        }
        finally
        {
            if ( saved == null ) System.clearProperty( key ); else System.setProperty( key, saved );
        }
    }

    /** Filters are contracted to be stateless and sharable, and are cached accordingly. */
    public void testTheFilterInstanceIsShared() throws Exception
    {
        PropertiesConfig cfg = filterCfg( DropFactoryKeysFilter.class );

        assertSame( ReferenceableUtils.getUnsafeInitialContextEnvFilter( cfg ),
                    ReferenceableUtils.getUnsafeInitialContextEnvFilter( cfg ) );

        clearFilterCache();
        assertNotSame( "Precondition for every other test here: clearing the cache really does "
                       + "force a fresh resolution.",
                       ReferenceableUtils.getUnsafeInitialContextEnvFilter( cfg ),
                       null );
    }

    /** A misspelled filter class name must fail plainly, not silently fall back to something. */
    public void testANonexistentFilterClassIsReported() throws Exception
    {
        Properties p = new Properties();
        p.setProperty( SecurityConfigKey.UNSAFE_INITIAL_CONTEXT_ENV_FILTER_CLASS_NAME, "com.example.NoSuchFilter" );
        try
        {
            ReferenceableUtils.getUnsafeInitialContextEnvFilter(
                MultiPropertiesConfig.fromProperties( "/notional-test-resource", p ) );
            fail( "Expected a ClassNotFoundException for a filter class that does not exist." );
        }
        catch ( ClassNotFoundException e ) { /* expected */ }
    }

    // ==================== what the shipped filters do ====================

    /**
     *  Returning null is not refusal: it discards the untrusted environment and lets the lookup
     *  proceed against the JVM default, exactly as new InitialContext() would. The distinction
     *  from throwing is the whole of the interface's return contract.
     */
    public void testReplaceWithDefaultYieldsANullEnvironment() throws Exception
    {
        UnsafeInitialContextEnvFilter filter = new AlwaysReplaceWithDefaultUnsafeInitialContextEnvFilter();

        assertNull( filter.safeEnv( hostileEnv(), getClass(), null ) );
    }

    public void testForbidRefusesEvenAnInnocuousEnvironment() throws Exception
    {
        Hashtable<String,String> harmless = new Hashtable<String,String>();
        harmless.put( "java.naming.batchsize", "10" );

        try
        {
            new AlwaysForbidUnsafeInitialContextEnvFilter().safeEnv( harmless, getClass(), null );
            fail( "AlwaysForbid judges provenance, not content." );
        }
        catch ( ForbiddenInitialContextException e ) { /* expected */ }
    }

    // ==================== what a bespoke filter can do ====================

    /**
     *  The documented middle path, and the reason the interface returns an environment rather
     *  than a boolean: a deployment can keep what it understands and drop what it does not.
     */
    public void testABespokeFilterCanReturnAFilteredEnvironment() throws Exception
    {
        UnsafeInitialContextEnvFilter filter =
            ReferenceableUtils.getUnsafeInitialContextEnvFilter( filterCfg( DropFactoryKeysFilter.class ) );

        Hashtable<?,?> out = filter.safeEnv( hostileEnv(), getClass(), null );

        assertNull( "The context factory names a class to instantiate and must be gone.",
                    out.get( "java.naming.factory.initial" ) );
        assertEquals( "Harmless tuning survives.", "10", out.get( "java.naming.batchsize" ) );
    }

    /** The filter sees the environment as it arrived, unmodified, so it can judge all of it. */
    public void testTheFilterSeesTheEnvironmentAsItArrived() throws Exception
    {
        RecordingFilter.lastEnv = null;
        Hashtable<String,String> env = hostileEnv();

        ReferenceableUtils.getUnsafeInitialContextEnvFilter( filterCfg( RecordingFilter.class ) )
            .safeEnv( env, getClass(), null );

        assertEquals( env, RecordingFilter.lastEnv );
        assertEquals( "ldap://evil.example.com:1389/payload",
                      RecordingFilter.lastEnv.get( "java.naming.provider.url" ) );
    }
}
