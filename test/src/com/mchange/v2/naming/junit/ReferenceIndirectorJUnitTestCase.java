package com.mchange.v2.naming.junit;

import java.io.IOException;
import java.util.*;
import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import junit.framework.TestCase;
import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.naming.AnyNameNameGuard;
import com.mchange.v2.naming.FirstComponentIsJavaIdentifierNameGuard;
import com.mchange.v2.cfg.SecurityRatchetTestSupport;
import com.mchange.v2.naming.AlwaysForbidUnsafeInitialContextEnvFilter;
import com.mchange.v2.naming.AlwaysReplaceWithDefaultUnsafeInitialContextEnvFilter;
import com.mchange.v2.naming.ForbiddenInitialContextException;
import com.mchange.v2.naming.ReferenceIndirector;
import com.mchange.v2.naming.UnsafeInitialContextEnvFilter;
import com.mchange.v2.naming.ReferenceableUtils;
import com.mchange.v2.naming.SecurityConfigKey;
import com.mchange.v2.ser.IndirectSerializationForbiddenException;
import com.mchange.v2.ser.IndirectlySerialized;

public final class ReferenceIndirectorJUnitTestCase extends TestCase
{
    // ==========================================
    // Test helpers: ObjectFactory + Referenceable
    // Must be public static for Class.forName + newInstance to work
    // ==========================================

    public static final class SimpleObjectFactory implements ObjectFactory
    {
        @Override
        public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment )
            throws Exception
        { return "SIMPLE"; }
    }

    static final String SIMPLE_FACTORY = SimpleObjectFactory.class.getName();

    /** A minimal Referenceable backed by SimpleObjectFactory. */
    public static final class TestReferenceable implements Referenceable
    {
        @Override
        public Reference getReference() throws NamingException
        { return new Reference( TestReferenceable.class.getName(), SIMPLE_FACTORY, null ); }
    }

    /**
     *  Records the environment it is handed, so we can tell which environment actually reached
     *  the ObjectFactory -- the one the filter returned, or the untrusted original.
     */
    public static final class EnvRecordingObjectFactory implements ObjectFactory
    {
        public static volatile Hashtable recordedEnv;
        public static volatile boolean   called;

        @Override
        public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment )
            throws Exception
        {
            recordedEnv = environment;
            called = true;
            return "RECORDED";
        }
    }

    static final String RECORDING_FACTORY = EnvRecordingObjectFactory.class.getName();

    public static final class RecordingReferenceable implements Referenceable
    {
        @Override
        public Reference getReference() throws NamingException
        { return new Reference( RecordingReferenceable.class.getName(), RECORDING_FACTORY, null ); }
    }

    /**
     *  An InitialContextFactory that records the environment JNDI hands it. This is how we
     *  observe the environment the InitialContext itself was built with, which is otherwise
     *  invisible: the context is constructed eagerly but only consults its environment when a
     *  lookup actually happens.
     */
    public static final class EnvRecordingInitialContextFactory implements javax.naming.spi.InitialContextFactory
    {
        public static volatile Hashtable recordedEnv;

        @Override
        public Context getInitialContext( Hashtable<?,?> environment )
        {
            recordedEnv = environment;
            return (Context) java.lang.reflect.Proxy.newProxyInstance(
                Context.class.getClassLoader(),
                new Class<?>[] { Context.class },
                new java.lang.reflect.InvocationHandler()
                {
                    @Override
                    public Object invoke( Object proxy, java.lang.reflect.Method m, Object[] args )
                    { return "lookup".equals( m.getName() ) ? proxy : null; }
                } );
        }
    }

    /** Keeps the context factory (so a lookup can happen at all) and drops everything else. */
    public static final class KeepOnlyFactoryEnvFilter implements UnsafeInitialContextEnvFilter
    {
        @Override
        public Hashtable<?,?> safeEnv( Hashtable<?,?> env, Class<?> materializingClass, PropertiesConfig pcfg )
        {
            Hashtable<Object,Object> out = new Hashtable<Object,Object>();
            out.put( Context.INITIAL_CONTEXT_FACTORY, EnvRecordingInitialContextFactory.class.getName() );
            return out;
        }
    }

    /** Replaces the whole environment with a single marker entry, so its effect is unmistakable. */
    public static final class MarkerEnvFilter implements UnsafeInitialContextEnvFilter
    {
        public final static String MARKER_KEY = "filtered.by.MarkerEnvFilter";

        @Override
        public Hashtable<?,?> safeEnv( Hashtable<?,?> env, Class<?> materializingClass, PropertiesConfig pcfg )
        {
            Hashtable<Object,Object> out = new Hashtable<Object,Object>();
            out.put( MARKER_KEY, "yes" );
            return out;
        }
    }

    // ==========================================
    // Helpers
    // ==========================================

    private static PropertiesConfig pcfg( String key, String value )
    {
        Properties p = new Properties();
        p.setProperty( key, value );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    private static PropertiesConfig pcfg( String key1, String val1, String key2, String val2 )
    {
        Properties p = new Properties();
        p.setProperty( key1, val1 );
        p.setProperty( key2, val2 );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    private static void restoreSystemProperty( String key, String savedValue )
    {
        if ( savedValue == null )
            System.clearProperty( key );
        else
            System.setProperty( key, savedValue );
    }

    /**
     * Use the given indirector to produce a ReferenceSerialized wrapping a TestReferenceable.
     */
    private static IndirectlySerialized makeReferenceSerialized( ReferenceIndirector indirector ) throws Exception
    { return indirector.indirectForm( new TestReferenceable() ); }

    // ==========================================
    // Open the gate for the bulk of the tests
    //
    // The actual functionality exercised below is unchanged by the new gating;
    // we set the allow-sysprop to "true" for the class as a whole so the existing
    // tests keep covering what they always did. A dedicated section further down
    // verifies the gate itself by toggling this sysprop and/or supplying a pcfg.
    // ==========================================

    private String savedAllowSysprop;

    /**
     *  Each case sets its own configuration and expects it to take effect, so each has to look
     *  like a fresh JVM. ReferenceableUtils now holds its security flags in static
     *  EarliestOrStrongestBooleanProperty instances, which latch at their safe value and stop
     *  consulting configuration -- deliberately, since the supported arrangement is to
     *  configure once at startup and leave it alone. Releasing them here is what lets a shared
     *  JVM stand in for several deployments.
     */
    @Override
    protected void setUp() throws Exception
    {
        SecurityRatchetTestSupport.resetAll( ReferenceableUtils.class );
        savedAllowSysprop = System.getProperty( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE );
        System.setProperty( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE, "true" );
    }

    @Override
    protected void tearDown() throws Exception
    {
        restoreSystemProperty( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE, savedAllowSysprop );
        SecurityRatchetTestSupport.resetAll( ReferenceableUtils.class );
    }

    // ==========================================
    // Getter / setter tests
    // ==========================================

    public void testGetNameDefaultNull()
    { assertNull( new ReferenceIndirector().getName() ); }

    public void testSetGetName() throws InvalidNameException
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        Name name = new CompositeName( "java:comp/env/myDS" );
        ri.setName( name );
        assertSame( name, ri.getName() );
    }

    public void testGetContextNameDefaultNull()
    { assertNull( new ReferenceIndirector().getNameContextName() ); }

    public void testSetGetContextName() throws InvalidNameException
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        Name ctxName = new CompositeName( "java:comp/env" );
        ri.setNameContextName( ctxName );
        assertSame( ctxName, ri.getNameContextName() );
    }

    public void testGetEnvironmentPropertiesDefaultNull()
    { assertNull( new ReferenceIndirector().getEnvironmentProperties() ); }

    public void testSetGetEnvironmentProperties()
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        Hashtable env = new Hashtable();
        env.put( "key", "value" );
        ri.setEnvironmentProperties( env );
        assertSame( env, ri.getEnvironmentProperties() );
    }

    // ==========================================
    // indirectForm() tests
    // ==========================================

    public void testIndirectFormReturnsNonNull() throws Exception
    { assertNotNull( makeReferenceSerialized( new ReferenceIndirector() ) ); }

    public void testIndirectFormReturnsIndirectlySerialized() throws Exception
    { assertTrue( makeReferenceSerialized( new ReferenceIndirector() ) instanceof IndirectlySerialized ); }

    public void testIndirectFormToStringContainsReferenceInfo() throws Exception
    {
        String str = makeReferenceSerialized( new ReferenceIndirector() ).toString();
        assertNotNull( str );
        // The ReferenceSerialized.toString() mentions "reference=", "name=", "contextName=", "env="
        assertTrue( "toString should contain 'reference'", str.toLowerCase().contains( "reference" ) );
    }

    // ==========================================
    // ReferenceSerialized.getObject() - security rejection tests
    // ==========================================

    /**
     * When the indirector was given a non-null environment and
     * acceptDeserializedInitialContextEnvironment is false (the default),
     * getObject() must throw IOException.
     */
    public void testGetObjectNonNullEnvRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setEnvironmentProperties( new Hashtable() );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        try
        {
            is.getObject( null );
            fail( "Expected IOException: non-null env rejected by default" );
        }
        catch (IOException e) { /* expected */ }
    }

    /**
     * When acceptDeserializedInitialContextEnvironment=true and a whitelist is provided,
     * an empty (but non-null) env is accepted and getObject() resolves the reference.
     */
    public void testGetObjectNonNullEnvPermittedByConfig() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setEnvironmentProperties( new Hashtable() );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg(
            SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, "true",
            SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY
        );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /**
     * "jdbc/DataSource" lacks a "java:" first component so is not explicitly local.
     * The default ApparentlyLocalNameGuard rejects it and getObject() must throw IOException.
     */
    public void testGetObjectNotExplicitlyLocalContextNameRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setNameContextName( new CompositeName( "jdbc/DataSource" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        try
        {
            is.getObject( null );
            fail( "Expected IOException: contextName rejected by default ApparentlyLocalNameGuard" );
        }
        catch (IOException e) { /* expected */ }
    }

    // ==========================================
    // The InitialContext environment filter
    //
    // An environment arriving on a deserialized or dereferenced object can name the context
    // factory to instantiate and the server to fetch from. acceptDeserializedInitialContextEnvironment
    // decides whether such an environment may be considered at all; the filter decides what it
    // may contain. Both must permit, and the filter is named by the deployment rather than by
    // the object carrying the environment.
    // ==========================================

    private static PropertiesConfig envFilterCfg( String filterClassName )
    {
        Properties p = new Properties();
        p.setProperty( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, "true" );
        p.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, RECORDING_FACTORY );
        if ( filterClassName != null )
            p.setProperty( SecurityConfigKey.UNSAFE_INITIAL_CONTEXT_ENV_FILTER_CLASS_NAME, filterClassName );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    private static Hashtable<String,String> hostileEnv()
    {
        Hashtable<String,String> env = new Hashtable<String,String>();
        env.put( "java.naming.provider.url", "ldap://evil.example.com:1389/payload" );
        return env;
    }

    private static IndirectlySerialized recordingSerialized( Hashtable<?,?> env ) throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setEnvironmentProperties( env );
        return ri.indirectForm( new RecordingReferenceable() );
    }

    /**
     *  The environment the ObjectFactory receives must be the filter's, not the original. The
     *  filtered environment used to reach only the InitialContext constructor, while the raw
     *  one was passed on to referenceToObject and from there to the factory -- so a filter
     *  could appear to be working while the thing it removed was delivered anyway.
     */
    public void testTheFilteredEnvironmentIsWhatReachesTheObjectFactory() throws Exception
    {
        EnvRecordingObjectFactory.called = false;
        EnvRecordingObjectFactory.recordedEnv = null;

        IndirectlySerialized is = recordingSerialized( hostileEnv() );
        assertEquals( "RECORDED", is.getObject( envFilterCfg( MarkerEnvFilter.class.getName() ) ) );

        assertTrue( "Precondition: the factory ran.", EnvRecordingObjectFactory.called );
        assertEquals( "The factory must see what the filter returned.",
                      "yes", EnvRecordingObjectFactory.recordedEnv.get( MarkerEnvFilter.MARKER_KEY ) );
        assertNull( "and must not see what the filter removed: " + EnvRecordingObjectFactory.recordedEnv,
                    EnvRecordingObjectFactory.recordedEnv.get( "java.naming.provider.url" ) );
    }

    /** Discarding the environment entirely leaves the factory with none, not with the original. */
    public void testReplacingWithTheDefaultLeavesTheFactoryWithNoEnvironment() throws Exception
    {
        EnvRecordingObjectFactory.called = false;
        EnvRecordingObjectFactory.recordedEnv = null;

        IndirectlySerialized is = recordingSerialized( hostileEnv() );
        assertEquals( "RECORDED",
            is.getObject( envFilterCfg( AlwaysReplaceWithDefaultUnsafeInitialContextEnvFilter.class.getName() ) ) );

        assertTrue( EnvRecordingObjectFactory.called );
        assertNull( "A discarded environment must not resurface downstream: "
                    + EnvRecordingObjectFactory.recordedEnv,
                    EnvRecordingObjectFactory.recordedEnv );
    }

    /**
     *  A refusal must be distinguishable from a mechanical failure. It arrives as an
     *  IOException either way, so the cause is what carries the distinction -- and the catch
     *  that classifies it is easy to get wrong, since a filter that throws and a filter that
     *  cannot be constructed arrive at the same place.
     */
    public void testARefusalIsReportedAsARefusalRatherThanAFailure() throws Exception
    {
        IndirectlySerialized is = recordingSerialized( hostileEnv() );
        try
        {
            is.getObject( envFilterCfg( AlwaysForbidUnsafeInitialContextEnvFilter.class.getName() ) );
            fail( "Expected IOException: the filter refuses this environment." );
        }
        catch ( IOException e )
        {
            assertTrue( "A refusal should be caused by the refusal, not by an instantiation failure: " + e.getCause(),
                        e.getCause() instanceof ForbiddenInitialContextException );
            assertTrue( "and should say so: " + e.getMessage(),
                        e.getMessage().indexOf( "policy decision" ) >= 0 );
        }
    }

    /** Unconfigured, the filter refuses -- so the boolean alone is no longer enough. */
    public void testWithNoFilterConfiguredAnEnvironmentIsStillRefused() throws Exception
    {
        IndirectlySerialized is = recordingSerialized( hostileEnv() );
        try
        {
            is.getObject( envFilterCfg( null ) );
            fail( "Expected IOException: acceptDeserializedInitialContextEnvironment no longer suffices alone." );
        }
        catch ( IOException e )
        { assertTrue( "" + e.getCause(), e.getCause() instanceof ForbiddenInitialContextException ); }
    }

    /**
     *  An empty environment is behaviourally identical to none -- new InitialContext(empty) and
     *  new InitialContext() do the same thing -- so it is normalized away and never reaches the
     *  filter. Without that, the default filter would refuse a reference carrying an empty
     *  Hashtable, which commons used to accept and which c3p0 has always treated as absent.
     */
    public void testAnEmptyEnvironmentIsTreatedAsAbsent() throws Exception
    {
        EnvRecordingObjectFactory.called = false;

        IndirectlySerialized is = recordingSerialized( new Hashtable() );

        // no filter configured, so the default would refuse if the empty env reached it
        assertEquals( "RECORDED", is.getObject( envFilterCfg( null ) ) );
        assertTrue( EnvRecordingObjectFactory.called );
    }

    /**
     *  And the normalization has to survive deserialization, which is how these objects
     *  actually arrive: Java deserialization assigns fields directly and runs no constructor,
     *  so normalizing at construction alone would leave a stream written by an older version
     *  -- or by anyone else -- carrying an empty Hashtable straight to the filter.
     */
    public void testAnEmptyEnvironmentArrivingByDeserializationIsAlsoTreatedAsAbsent() throws Exception
    {
        EnvRecordingObjectFactory.called = false;

        IndirectlySerialized is = recordingSerialized( null );

        // stand in for a stream written before empty environments were normalized away
        java.lang.reflect.Field f = is.getClass().getDeclaredField( "env" );
        f.setAccessible( true );
        f.set( is, new Hashtable() );

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream( baos );
        try { oos.writeObject( is ); } finally { oos.close(); }
        java.io.ObjectInputStream ois =
            new java.io.ObjectInputStream( new java.io.ByteArrayInputStream( baos.toByteArray() ) );
        IndirectlySerialized restored;
        try { restored = (IndirectlySerialized) ois.readObject(); } finally { ois.close(); }

        assertEquals( "RECORDED", restored.getObject( envFilterCfg( null ) ) );
        assertTrue( EnvRecordingObjectFactory.called );
    }

    /**
     *  The InitialContext must be built from the filtered environment too. That is harder to
     *  observe than the factory case, because the context is only consulted when a lookup
     *  happens -- so this one sets a contextName, which forces the lookup, and routes it
     *  through a context factory that records the environment JNDI passed it.
     */
    public void testTheInitialContextIsBuiltFromTheFilteredEnvironment() throws Exception
    {
        EnvRecordingInitialContextFactory.recordedEnv = null;

        Hashtable<String,String> env = new Hashtable<String,String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, EnvRecordingInitialContextFactory.class.getName() );
        env.put( "java.naming.provider.url", "ldap://evil.example.com:1389/payload" );

        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setEnvironmentProperties( env );
        ri.setNameContextName( new CompositeName( "java:comp/env" ) ); // local, so the NameGuard allows it
        IndirectlySerialized is = ri.indirectForm( new RecordingReferenceable() );

        is.getObject( envFilterCfg( KeepOnlyFactoryEnvFilter.class.getName() ) );

        assertNotNull( "Precondition: a lookup happened, so the context consulted its environment.",
                       EnvRecordingInitialContextFactory.recordedEnv );
        assertNull( "The InitialContext must not be built from the unfiltered environment: "
                    + EnvRecordingInitialContextFactory.recordedEnv,
                    EnvRecordingInitialContextFactory.recordedEnv.get( "java.naming.provider.url" ) );
    }

    // ==========================================
    // ReferenceSerialized.getObject() - success tests
    // ==========================================

    /** Simplest success case: null env, null contextName, factory whitelisted in pcfg. */
    public void testGetObjectWithWhitelistedFactoryViaPcfg() throws Exception
    {
        IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /** No-arg getObject() uses the system-property whitelist when present. */
    public void testGetObjectNoArgWithWhitelistViaSysprop() throws Exception
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
            IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );
            assertEquals( "SIMPLE", is.getObject() );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * A local name (first component starts with "java:") set on the indirector
     * is acceptable by default and getObject() succeeds with a whitelisted factory.
     */
    public void testGetObjectWithLocalNameSetSucceeds() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "java:comp/env/myObj" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /**
     * "jdbc/DataSource" lacks a "java:" prefix so is not explicitly local.
     * The default ApparentlyLocalNameGuard rejects it; the NamingException is wrapped as IOException.
     */
    public void testGetObjectWithNotExplicitlyLocalNameRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "jdbc/DataSource" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
        try
        {
            is.getObject( cfg );
            fail( "Expected IOException: jdbc/DataSource rejected by default ApparentlyLocalNameGuard" );
        }
        catch (IOException e) { /* expected */ }
    }

    /**
     * "jdbc/DataSource" lacks a "java:" prefix so is rejected by the default guard,
     * but is accepted when FirstComponentIsJavaIdentifierNameGuard is configured
     * (its first component "jdbc" is a valid Java identifier).
     */
    public void testGetObjectWithNotExplicitlyLocalNamePermittedByConfig() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "jdbc/DataSource" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg(
            SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY,
            SecurityConfigKey.NAME_GUARD_CLASS_NAME, FirstComponentIsJavaIdentifierNameGuard.class.getName()
        );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /**
     * A contextName with scheme "ldap:" is genuinely non-local.
     * The default ApparentlyLocalNameGuard rejects it and getObject() must throw IOException.
     */
    public void testGetObjectNonLocalContextNameRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setNameContextName( new CompositeName( "ldap://example.com/ctx" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        try
        {
            is.getObject( null );
            fail( "Expected IOException: ldap:// contextName rejected by default ApparentlyLocalNameGuard" );
        }
        catch (IOException e) { /* expected */ }
    }

    /**
     * A name with scheme "ldap:" is genuinely non-local.
     * The default ApparentlyLocalNameGuard rejects it; the NamingException is wrapped as IOException.
     */
    public void testGetObjectWithNonLocalNameRejectedByDefault() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "ldap://example.com/myObj" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY );
        try
        {
            is.getObject( cfg );
            fail( "Expected IOException: ldap:// name rejected by default ApparentlyLocalNameGuard" );
        }
        catch (IOException e) { /* expected */ }
    }

    /**
     * A genuinely non-local "ldap://..." name is accepted when AnyNameNameGuard is configured,
     * and dereferencing succeeds with a whitelisted factory.
     */
    public void testGetObjectWithNonLocalNamePermittedByConfig() throws Exception
    {
        ReferenceIndirector ri = new ReferenceIndirector();
        ri.setName( new CompositeName( "ldap://example.com/myObj" ) );
        IndirectlySerialized is = makeReferenceSerialized( ri );
        PropertiesConfig cfg = pcfg(
            SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY,
            SecurityConfigKey.NAME_GUARD_CLASS_NAME, AnyNameNameGuard.class.getName()
        );
        assertEquals( "SIMPLE", is.getObject( cfg ) );
    }

    /**
     * Without any whitelist configured (neither pcfg nor system property),
     * getObject() must fail because the mandatory whitelist is missing.
     * The NamingException is wrapped in an InvalidObjectException (an IOException subclass).
     */
    public void testGetObjectNoWhitelistFails() throws Exception
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.clearProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
            IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );
            // pass an empty pcfg that has no whitelist key
            PropertiesConfig cfg = MultiPropertiesConfig.fromProperties( "/test", new Properties() );
            is.getObject( cfg );
            fail( "Expected IOException: no whitelist configured" );
        }
        catch (IOException e) { /* expected: NamingException wrapped in InvalidObjectException */ }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * When the factory class is not in the whitelist, getObject() must fail.
     * The NamingException is wrapped in an InvalidObjectException (an IOException subclass).
     */
    public void testGetObjectFactoryNotInWhitelistFails() throws Exception
    {
        IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );
        // whitelist only contains a different factory
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "com.example.SomeOtherFactory" );
        try
        {
            is.getObject( cfg );
            fail( "Expected IOException: factory not in whitelist" );
        }
        catch (IOException e) { /* expected */ }
    }

    // ==========================================
    // Gate tests: ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE
    //
    // setUp() sets the allow-sysprop to "true"; individual tests below clear or
    // override it to exercise the gate. tearDown() restores the original value.
    // ==========================================

    /** With the sysprop cleared and no pcfg supplied, indirectForm(orig) must refuse. */
    public void testIndirectFormForbiddenWhenGateClosed() throws Exception
    {
        System.clearProperty( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE );
        try
        {
            new ReferenceIndirector().indirectForm( new TestReferenceable() );
            fail( "Expected IndirectSerializationForbiddenException with the gate closed" );
        }
        catch (IndirectSerializationForbiddenException e) { /* expected */ }
    }

    /** With the sysprop cleared and no pcfg supplied, getObject(null) must refuse. */
    public void testGetObjectForbiddenWhenGateClosed() throws Exception
    {
        // The ReferenceSerialized is produced while the gate is open (sysprop=true from setUp()).
        IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );

        // Decoding happens in a deployment of its own -- which is the realistic case anyway, since
        // the artifact an attacker holds was produced somewhere else. System properties are read as
        // of the seal, so closing the gate means closing it before this deployment reads it.
        SecurityRatchetTestSupport.resetAll( ReferenceableUtils.class );
        System.clearProperty( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE );
        try
        {
            is.getObject( null );
            fail( "Expected IndirectSerializationForbiddenException with the gate closed" );
        }
        catch (IndirectSerializationForbiddenException e) { /* expected */ }
    }

    /**
     * Sysprop unset, but pcfg passed to the pcfg-aware indirectForm() opts in.
     * Exercises the new indirectForm(orig, pcfg) overload.
     */
    public void testIndirectFormAllowedByPcfgWhenSyspropAbsent() throws Exception
    {
        System.clearProperty( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE, "true" );
        IndirectlySerialized is = new ReferenceIndirector().indirectForm( new TestReferenceable(), cfg );
        assertNotNull( is );
    }

    /** Sysprop unset, but pcfg passed to getObject(pcfg) opts in; full resolution succeeds. */
    public void testGetObjectAllowedByPcfgWhenSyspropAbsent() throws Exception
    {
        PropertiesConfig openCfg = pcfg(
            SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE, "true",
            SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY
        );
        // Produce the ReferenceSerialized using the pcfg-aware overload, then clear the sysprop.
        IndirectlySerialized is = new ReferenceIndirector().indirectForm( new TestReferenceable(), openCfg );
        System.clearProperty( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE );
        assertEquals( "SIMPLE", is.getObject( openCfg ) );
    }

    /** Sysprop=false must veto pcfg=true on the serialize side. */
    public void testIndirectFormSyspropFalseOverridesPcfgTrue() throws Exception
    {
        System.setProperty( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE, "false" );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE, "true" );
        try
        {
            new ReferenceIndirector().indirectForm( new TestReferenceable(), cfg );
            fail( "Expected IndirectSerializationForbiddenException: sysprop=false must override pcfg=true" );
        }
        catch (IndirectSerializationForbiddenException e) { /* expected */ }
    }

    /** Sysprop=false must veto pcfg=true on the deserialize side. */
    public void testGetObjectSyspropFalseOverridesPcfgTrue() throws Exception
    {
        // Produce the ReferenceSerialized while the gate is open (sysprop=true from setUp()).
        IndirectlySerialized is = makeReferenceSerialized( new ReferenceIndirector() );

        // Again a separate deployment, whose operator has pinned the flag safe before anything reads it.
        SecurityRatchetTestSupport.resetAll( ReferenceableUtils.class );
        System.setProperty( SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE, "false" );
        PropertiesConfig cfg = pcfg(
            SecurityConfigKey.ALLOW_INDIRECT_SERIALIZATION_VIA_REFERENCE, "true",
            SecurityConfigKey.OBJECT_FACTORY_WHITELIST, SIMPLE_FACTORY
        );
        try
        {
            is.getObject( cfg );
            fail( "Expected IndirectSerializationForbiddenException: sysprop=false must override pcfg=true" );
        }
        catch (IndirectSerializationForbiddenException e) { /* expected */ }
    }
}
