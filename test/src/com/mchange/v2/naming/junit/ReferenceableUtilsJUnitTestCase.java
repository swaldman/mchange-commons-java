package com.mchange.v2.naming.junit;

import java.util.*;
import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import junit.framework.TestCase;
import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.naming.AnyNameNameGuard;
import com.mchange.v2.naming.NameGuard;
import com.mchange.v2.naming.ApparentlyLocalNameGuard;
import com.mchange.v2.naming.ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard;
import com.mchange.v2.naming.FirstComponentIsJavaIdentifierNameGuard;
import com.mchange.v2.cfg.SecurityRatchetTestSupport;
import com.mchange.v2.naming.ReferenceableUtils;
import com.mchange.v2.naming.SecurityConfigKey;

public final class ReferenceableUtilsJUnitTestCase extends TestCase
{
    // ==========================================
    // Test ObjectFactory implementations
    // Must be public static for Class.forName + newInstance to work
    // ==========================================

    public static final class AlphaObjectFactory implements ObjectFactory
    {
        @Override
        public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment )
            throws Exception
        { return "ALPHA"; }
    }

    public static final class BetaObjectFactory implements ObjectFactory
    {
        @Override
        public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment )
            throws Exception
        { return "BETA"; }
    }

    // Binary names as used by Class.forName / class literals
    static final String DEFAULT_NAME_GUARD_CLASS_NAME = ApparentlyLocalNameGuard.class.getName();

    static final String ALPHA_FACTORY = AlphaObjectFactory.class.getName();
    static final String BETA_FACTORY  = BetaObjectFactory.class.getName();

    // ==========================================
    // Helpers
    // ==========================================

    private static Reference makeRef( String className, String factoryClassName )
    { return new Reference( className, factoryClassName, null ); }

    /** Build a PropertiesConfig with a single key/value pair. */
    private static PropertiesConfig pcfg( String key, String value )
    {
        Properties p = new Properties();
        p.setProperty( key, value );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    /**
     *  See ReferenceIndirectorJUnitTestCase: the security flags latch once read, so each case
     *  must begin from an unstarted JVM's worth of state.
     */
    @Override
    protected void setUp() throws Exception
    { freshJvmState(); }

    @Override
    protected void tearDown() throws Exception
    { freshJvmState(); }

    /**
     *  NameGuards are resolved once per class name and the instance is kept, since the interface
     *  contracts them to be stateless and substitutable. That cache is static and lives as long
     *  as the JVM, so without clearing it a case can be satisfied by an instance some earlier
     *  case created -- and any case about how guards are <i>resolved</i> would quietly stop
     *  exercising resolution at all. That is not hypothetical: the filter suite had exactly this
     *  defect, and only a mutation that should have broken it revealed the tests were idle.
     */
    private static void freshJvmState() throws Exception
    {
        SecurityRatchetTestSupport.resetAll( ReferenceableUtils.class );

        // enforceWhitelist lives in ByNameInstantiationUtils rather than here, so resetting only
        // ReferenceableUtils would leave it latched at whatever an earlier case left it -- which
        // is how the filter suite's enforcement test came to pass while exercising nothing.
        SecurityRatchetTestSupport.resetAll( com.mchange.v2.reflect.ByNameInstantiationUtils.class );

        java.lang.reflect.Field f =
            ReferenceableUtils.class.getDeclaredField( "nameGuardClassNameToInstance" );
        f.setAccessible( true );
        ((Map<?,?>) f.get( null )).clear();
    }

    private static void restoreSystemProperty( String key, String savedValue )
    {
        if ( savedValue == null )
            System.clearProperty( key );
        else
            System.setProperty( key, savedValue );
    }

    // ==========================================
    // literalNullToNull
    // ==========================================

    public void testLiteralNullToNullWithNull()
    { assertNull( ReferenceableUtils.literalNullToNull( null ) ); }

    public void testLiteralNullToNullWithLiteralNull()
    { assertNull( ReferenceableUtils.literalNullToNull( "null" ) ); }

    public void testLiteralNullToNullPassthrough()
    { assertEquals( "hello", ReferenceableUtils.literalNullToNull( "hello" ) ); }

    public void testLiteralNullToNullEmptyString()
    { assertEquals( "", ReferenceableUtils.literalNullToNull( "" ) ); }

    // ==========================================
    // AnyNameNameGuard
    // Accepts every name unconditionally.
    // ==========================================

    public void testAnyNameGuardAcceptsAnyString()
    {
        AnyNameNameGuard guard = new AnyNameNameGuard();
        assertTrue( guard.nameIsAcceptable( "" ) );
        assertTrue( guard.nameIsAcceptable( "java:comp/env" ) );
        assertTrue( guard.nameIsAcceptable( "ldap://example.com/myDS" ) );
        assertTrue( guard.nameIsAcceptable( "anything at all" ) );
    }

    public void testAnyNameGuardAcceptsAnyName() throws InvalidNameException
    {
        AnyNameNameGuard guard = new AnyNameNameGuard();
        assertTrue( guard.nameIsAcceptable( new CompositeName( "" ) ) );
        assertTrue( guard.nameIsAcceptable( new CompositeName( "java:comp/env" ) ) );
        assertTrue( guard.nameIsAcceptable( new CompositeName( "ldap://example.com" ) ) );
    }

    public void testAnyNameGuardDescriptionNonNull()
    { assertNotNull( new AnyNameNameGuard().onlyAcceptableWhen() ); }

    // ==========================================
    // ApparentlyLocalNameGuard
    // String: must start with "java:"
    // Name:   first component must start with "java:"
    // ==========================================

    public void testApparentlyLocalNameGuardStringLocal()
    {
        ApparentlyLocalNameGuard guard = new ApparentlyLocalNameGuard();
        assertTrue( guard.nameIsAcceptable( "java:comp/env/myDS" ) );
        assertTrue( guard.nameIsAcceptable( "java:" ) );
    }

    public void testApparentlyLocalNameGuardStringNonLocal()
    {
        ApparentlyLocalNameGuard guard = new ApparentlyLocalNameGuard();
        assertFalse( guard.nameIsAcceptable( "" ) );
        assertFalse( guard.nameIsAcceptable( "ldap://example.com" ) );
        assertFalse( guard.nameIsAcceptable( "jdbc/myDS" ) );
        assertFalse( guard.nameIsAcceptable( "jms/topic" ) );
    }

    public void testApparentlyLocalNameGuardNameLocalFirstComponent() throws InvalidNameException
    {
        ApparentlyLocalNameGuard guard = new ApparentlyLocalNameGuard();
        // CompositeName splits on "/", so first component of "java:comp/env" is "java:comp"
        assertTrue( guard.nameIsAcceptable( new CompositeName( "java:comp/env" ) ) );
        assertTrue( guard.nameIsAcceptable( new CompositeName( "java:" ) ) );
    }

    public void testApparentlyLocalNameGuardNameNonLocal() throws InvalidNameException
    {
        ApparentlyLocalNameGuard guard = new ApparentlyLocalNameGuard();
        assertFalse( guard.nameIsAcceptable( new CompositeName( "" ) ) );          // empty Name
        assertFalse( guard.nameIsAcceptable( new CompositeName( "ldap://example.com" ) ) ); // first comp "ldap:"
        assertFalse( guard.nameIsAcceptable( new CompositeName( "jdbc/myDS" ) ) ); // first comp "jdbc"
    }

    public void testApparentlyLocalNameGuardDescriptionNonNull()
    { assertNotNull( new ApparentlyLocalNameGuard().onlyAcceptableWhen() ); }

    // ==========================================
    // FirstComponentIsJavaIdentifierNameGuard
    // String: text before the first "/" must be a valid Java (qualified) name.
    // Name:   first component must be a valid Java name.
    // ==========================================

    public void testFirstComponentJavaIdentifierGuardStringValid()
    {
        FirstComponentIsJavaIdentifierNameGuard guard = new FirstComponentIsJavaIdentifierNameGuard();
        assertTrue( guard.nameIsAcceptable( "jdbc/myDS" ) );   // first comp = "jdbc"
        assertTrue( guard.nameIsAcceptable( "jms/topic" ) );   // first comp = "jms"
        assertTrue( guard.nameIsAcceptable( "jdbc" ) );        // no slash; whole string = "jdbc"
    }

    public void testFirstComponentJavaIdentifierGuardStringInvalid()
    {
        FirstComponentIsJavaIdentifierNameGuard guard = new FirstComponentIsJavaIdentifierNameGuard();
        // "java:comp" before "/" contains a colon → not a valid Java name
        assertFalse( guard.nameIsAcceptable( "java:comp/env" ) );
        // bare "java:" has a colon → invalid
        assertFalse( guard.nameIsAcceptable( "java:" ) );
        // "ldap:" before first "/" has colon → invalid
        assertFalse( guard.nameIsAcceptable( "ldap://example.com" ) );
        assertFalse( guard.nameIsAcceptable( "" ) );
    }

    public void testFirstComponentJavaIdentifierGuardNameValid() throws InvalidNameException
    {
        FirstComponentIsJavaIdentifierNameGuard guard = new FirstComponentIsJavaIdentifierNameGuard();
        // CompositeName splits on "/"; first component is "jdbc"
        assertTrue( guard.nameIsAcceptable( new CompositeName( "jdbc/myDS" ) ) );
        assertTrue( guard.nameIsAcceptable( new CompositeName( "jms" ) ) );
    }

    public void testFirstComponentJavaIdentifierGuardNameInvalid() throws InvalidNameException
    {
        FirstComponentIsJavaIdentifierNameGuard guard = new FirstComponentIsJavaIdentifierNameGuard();
        // First component of "java:comp/env" is "java:comp" → colon → invalid
        assertFalse( guard.nameIsAcceptable( new CompositeName( "java:comp/env" ) ) );
        // Empty Name
        assertFalse( guard.nameIsAcceptable( new CompositeName( "" ) ) );
    }

    public void testFirstComponentJavaIdentifierGuardDescriptionNonNull()
    { assertNotNull( new FirstComponentIsJavaIdentifierNameGuard().onlyAcceptableWhen() ); }

    // ==========================================
    // ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard
    // Accepts if EITHER ApparentlyLocalNameGuard OR
    // FirstComponentIsJavaIdentifierNameGuard accepts.
    // ==========================================

    public void testApparentlyLocalOrJavaIdentifierGuardStringAccepted()
    {
        ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard guard =
            new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard();
        // Accepted by ApparentlyLocal
        assertTrue( guard.nameIsAcceptable( "java:comp/env" ) );
        // Accepted by FirstComponentIsJavaIdentifier
        assertTrue( guard.nameIsAcceptable( "jdbc/myDS" ) );
        assertTrue( guard.nameIsAcceptable( "jms/topic" ) );
    }

    public void testApparentlyLocalOrJavaIdentifierGuardStringRejected()
    {
        ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard guard =
            new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard();
        // Neither guard accepts these
        assertFalse( guard.nameIsAcceptable( "ldap://example.com" ) );
        assertFalse( guard.nameIsAcceptable( "" ) );
    }

    public void testApparentlyLocalOrJavaIdentifierGuardNameAccepted() throws InvalidNameException
    {
        ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard guard =
            new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard();
        // First component "java:comp" starts with "java:" → ApparentlyLocal accepts
        assertTrue( guard.nameIsAcceptable( new CompositeName( "java:comp/env" ) ) );
        // First component "jdbc" is valid Java name → FirstComponentIsJavaIdentifier accepts
        assertTrue( guard.nameIsAcceptable( new CompositeName( "jdbc/myDS" ) ) );
    }

    public void testApparentlyLocalOrJavaIdentifierGuardNameRejected() throws InvalidNameException
    {
        ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard guard =
            new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard();
        // First component "ldap:" has colon → neither accepts
        assertFalse( guard.nameIsAcceptable( new CompositeName( "ldap://example.com" ) ) );
        assertFalse( guard.nameIsAcceptable( new CompositeName( "" ) ) );
    }

    public void testApparentlyLocalOrJavaIdentifierGuardDescriptionNonNull()
    { assertNotNull( new ApparentlyLocalOrFirstComponentIsJavaIdentifierNameGuard().onlyAcceptableWhen() ); }

    // ==========================================
    // assertAcceptableName
    // Default NameGuard is ApparentlyLocalNameGuard.
    // NAME_GUARD_CLASS_NAME config overrides the guard.
    // When pcfg is non-null, pcfg is consulted; otherwise the system property is consulted.
    // ==========================================

    /** Default guard (ApparentlyLocalNameGuard): local String passes. */
    public void testAssertAcceptableNameDefaultGuardStringLocalPasses() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            ReferenceableUtils.assertAcceptableName( "java:comp/env/myDS", null );
        }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** Default guard: non-local String throws NamingException. */
    public void testAssertAcceptableNameDefaultGuardStringNonLocalThrows()
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            ReferenceableUtils.assertAcceptableName( "ldap://example.com", null );
            fail( "Expected NamingException: non-local name rejected by default guard" );
        }
        catch (NamingException e) { /* expected */ }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** Default guard: Name with "java:" first component passes. */
    public void testAssertAcceptableNameDefaultGuardNameLocalPasses() throws NamingException, InvalidNameException
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            ReferenceableUtils.assertAcceptableName( new CompositeName( "java:comp/env" ), null );
        }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** Default guard: Name without "java:" first component throws NamingException. */
    public void testAssertAcceptableNameDefaultGuardNameNonLocalThrows() throws InvalidNameException
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            ReferenceableUtils.assertAcceptableName( new CompositeName( "ldap://example.com" ), null );
            fail( "Expected NamingException: non-local Name rejected by default guard" );
        }
        catch (NamingException e) { /* expected */ }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** Unknown type (not String, not Name) always throws NamingException. */
    public void testAssertAcceptableNameUnknownTypeThrows()
    {
        try
        {
            ReferenceableUtils.assertAcceptableName( Integer.valueOf(42), null );
            fail( "Expected NamingException: unknown type" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** AnyNameNameGuard configured via pcfg: any String passes. */
    public void testAssertAcceptableNameAnyGuardViaPcfgAcceptsAnyString() throws NamingException
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME, AnyNameNameGuard.class.getName() );
        ReferenceableUtils.assertAcceptableName( "ldap://example.com", cfg );
        ReferenceableUtils.assertAcceptableName( "", cfg );
        ReferenceableUtils.assertAcceptableName( "java:comp/env", cfg );
    }

    /** AnyNameNameGuard configured via pcfg: any Name passes. */
    public void testAssertAcceptableNameAnyGuardViaPcfgAcceptsAnyName() throws NamingException, InvalidNameException
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME, AnyNameNameGuard.class.getName() );
        ReferenceableUtils.assertAcceptableName( new CompositeName( "ldap://example.com" ), cfg );
        ReferenceableUtils.assertAcceptableName( new CompositeName( "" ), cfg );
    }

    /** AnyNameNameGuard configured via system property: any String passes. */
    public void testAssertAcceptableNameAnyGuardViaSysprop() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.setProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, AnyNameNameGuard.class.getName() );
            ReferenceableUtils.assertAcceptableName( "ldap://example.com", null );
            ReferenceableUtils.assertAcceptableName( "", null );
        }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** FirstComponentIsJavaIdentifierNameGuard via pcfg: "jdbc/..." passes, "java:..." throws. */
    public void testAssertAcceptableNameFirstComponentGuardViaPcfg() throws NamingException
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                     FirstComponentIsJavaIdentifierNameGuard.class.getName() );
        // "jdbc/myDS" first component "jdbc" is a valid Java name → passes
        ReferenceableUtils.assertAcceptableName( "jdbc/myDS", cfg );

        // "java:comp/env" first component "java:comp" has a colon → throws
        try
        {
            ReferenceableUtils.assertAcceptableName( "java:comp/env", cfg );
            fail( "Expected NamingException: 'java:comp/env' rejected by FirstComponentIsJavaIdentifier guard" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Configuring a non-existent class name throws NamingException (not InternalError). */
    public void testAssertAcceptableNameBadGuardClassThrows()
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                     "com.example.DoesNotExistNameGuard" );
        try
        {
            ReferenceableUtils.assertAcceptableName( "java:comp/env", cfg );
            fail( "Expected NamingException: non-existent NameGuard class" );
        }
        catch (NamingException e) { /* expected */ }
    }

    // ==========================================
    // assertAcceptableName -- how a refusal describes the guard that refused
    //
    // A refusal is often the only thing a deployer sees, and it has to say where the guard came
    // from: "we defaulted to this" and "you configured this" call for different responses. That
    // provenance is decided by comparing the resolved class name against the default, which is
    // easy to get wrong -- the test was a null check for as long as an unconfigured lookup
    // returned null, and silently stopped meaning anything when the default moved into the
    // property object and null stopped being possible.
    // ==========================================

    /** Unconfigured: the refusal must own the choice as ours, not attribute it to the deployer. */
    public void testRefusalUnderTheDefaultGuardCallsItTheDefault()
    {
        String saved = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            ReferenceableUtils.assertAcceptableName( "ldap://example.com", null );
            fail( "Expected NamingException: non-local name rejected by default guard" );
        }
        catch (NamingException e)
        {
            String msg = e.getMessage();
            assertTrue( "Should name the default guard as a default: " + msg,
                        msg.contains( "default NameGuard" ) );
            assertTrue( "Nothing was configured, so nothing should be blamed on configuration: " + msg,
                        !msg.contains( "currently configured via" ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, saved ); }
    }

    /** Configured: the refusal must name the key, so the deployer knows what to go change. */
    public void testRefusalUnderAConfiguredGuardNamesTheKeyThatSelectedIt()
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                     FirstComponentIsJavaIdentifierNameGuard.class.getName() );
        try
        {
            // first component "java:comp" contains a colon, so this guard refuses it
            ReferenceableUtils.assertAcceptableName( "java:comp/env", cfg );
            fail( "Expected NamingException: 'java:comp/env' rejected by FirstComponentIsJavaIdentifier guard" );
        }
        catch (NamingException e)
        {
            String msg = e.getMessage();
            assertTrue( "Should name the guard that was configured: " + msg,
                        msg.contains( FirstComponentIsJavaIdentifierNameGuard.class.getName() ) );
            assertTrue( "and the key that selected it: " + msg,
                        msg.contains( SecurityConfigKey.NAME_GUARD_CLASS_NAME ) );
            assertTrue( "This guard was chosen, not defaulted to: " + msg,
                        !msg.contains( "default NameGuard" ) );
        }
    }

    /**
     *  A guard named in configuration that cannot be constructed is the deployer's problem, and
     *  the message says so. Its counterpart -- failing to construct the default, which would be
     *  ours and raises InternalError -- has no test: it would require ApparentlyLocalNameGuard
     *  to be absent from a JVM that is running this suite out of the same jar.
     */
    public void testAFailureToConstructAConfiguredGuardIsAttributedToConfiguration()
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                     "com.example.DoesNotExistNameGuard" );
        try
        {
            ReferenceableUtils.assertAcceptableName( "java:comp/env", cfg );
            fail( "Expected NamingException: non-existent NameGuard class" );
        }
        catch (NamingException e)
        {
            String msg = e.getMessage();
            assertTrue( "Should say the configured guard failed, and name it: " + msg,
                        msg.contains( "configured NameGuard" ) && msg.contains( "com.example.DoesNotExistNameGuard" ) );
        }
    }

    /**
     *  ApparentlyLocalNameGuard is the most restrictive guard we ship, and almost nobody
     *  configures a NameGuard at all, so falling back to it is the normal state rather than
     *  news. It must therefore be declared a high-security default, or every deployment that
     *  ever resolves a JNDI name gets a WARNING for being correctly configured.
     *
     *  <p>Asserted structurally, which is not how we would prefer to test it. The behaviour it
     *  protects -- that nothing is logged -- happens through ReferenceableUtils' own static
     *  MLogger, which a test cannot substitute for, and the declaration is the whole of what
     *  this class contributes; {@code SealedSystemPropertiesStringPropertyInternalJUnitTestCase}
     *  covers what the flag then does.</p>
     */
    public void testTheDefaultNameGuardIsDeclaredAHighSecurityDefault() throws Exception
    {
        java.lang.reflect.Field pf = ReferenceableUtils.class.getDeclaredField( "nameGuardClassNameProperty" );
        pf.setAccessible( true );
        Object prop = pf.get( null );

        java.lang.reflect.Field hsf = prop.getClass().getDeclaredField( "highSecurityDefault" );
        hsf.setAccessible( true );

        assertTrue( "Defaulting to the most restrictive guard we have is not something to warn about.",
                    hsf.getBoolean( prop ) );
    }

    /**
     *  NameGuards are resolved once and shared. That is a constraint on implementations -- they
     *  must be stateless and substitutable -- and worth pinning, because the contract used to be
     *  the opposite: a Constructor was cached and a fresh guard built for every name checked.
     *  Anything that silently went back to per-call construction would cost a reflective
     *  newInstance on every JNDI name check without failing anything.
     */
    public void testTheNameGuardInstanceIsShared() throws Exception
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME, AnyNameNameGuard.class.getName() );

        NameGuard first = nameGuardFor( cfg );
        assertSame( "A second resolution of the same class name should hand back the same instance.",
                    first, nameGuardFor( cfg ) );

        freshJvmState();
        assertNotSame( "and clearing the cache must force a fresh one -- which the rest of this "
                       + "case depends on, since a cached instance means resolution is not happening.",
                       first, nameGuardFor( cfg ) );
    }

    /**
     *  Resolution must survive whitelist enforcement. The guard class name comes from deployment
     *  configuration rather than from an untrusted object, which is the documented case for
     *  instantiating ungated -- and gating it would leave the shipped default unusable in exactly
     *  the deployments that had hardened themselves.
     */
    public void testNameGuardResolutionIsNotSubjectToTheByNameWhitelist() throws Exception
    {
        String key = "com.mchange.v2.reflect.byNameInstantiation.enforceWhitelist";
        String saved = System.getProperty( key );
        try
        {
            System.setProperty( key, "true" );
            freshJvmState(); // so the seal, and enforceWhitelist, pick it up

            // unconfigured, so this resolves the shipped default
            ReferenceableUtils.assertAcceptableName( "java:comp/env/myDS", null );

            assertEquals( "A guard the deployment named must resolve too.",
                          AnyNameNameGuard.class,
                          nameGuardFor( pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                              AnyNameNameGuard.class.getName() ) ).getClass() );
        }
        finally
        {
            restoreSystemProperty( key, saved );
        }
    }

    /** Resolve a NameGuard the way assertAcceptableName does, so the cache is exercised. */
    private static NameGuard nameGuardFor( PropertiesConfig cfg ) throws Exception
    {
        java.lang.reflect.Method m =
            ReferenceableUtils.class.getDeclaredMethod( "nameGuardForClassName", String.class );
        m.setAccessible( true );

        java.lang.reflect.Field pf =
            ReferenceableUtils.class.getDeclaredField( "nameGuardClassNameProperty" );
        pf.setAccessible( true );
        Object prop = pf.get( null );
        java.lang.reflect.Method gv = prop.getClass().getMethod(
            "getValue", PropertiesConfig.class, com.mchange.v2.log.MLogger.class );
        gv.setAccessible( true );
        String fqcn = (String) gv.invoke( prop, cfg, com.mchange.v2.log.MLog.getLogger( ReferenceableUtils.class ) );

        return (NameGuard) m.invoke( null, fqcn );
    }

    // There is deliberately no test for the InternalError branch of assertAcceptableName, the one
    // that fires when the *default* NameGuard itself will not construct -- our bug rather than the
    // deployer's. Reaching it requires construction of ApparentlyLocalNameGuard to fail, and that
    // class ships in the same jar as the code under test.
    //
    // There was one, which poisoned ReferenceableUtils' Constructor cache with a constructor that
    // throws. That cache now holds already-constructed instances, so there is nothing left to
    // poison: planting an instance makes construction succeed, which is the opposite of what the
    // test needed. Nor is there another seam -- the cache's get/put cannot throw checked
    // exceptions, and ByNameInstantiationUtils resolves against its own ClassLoader rather than
    // the thread context one.
    //
    // The other half of the attribution is still covered:
    // testAFailureToConstructAConfiguredGuardIsAttributedToConfiguration pins that a guard the
    // deployment named, and which cannot be constructed, is reported as theirs and not ours.

    /**
     *  Provenance is decided by comparing class names, so explicitly configuring the very class
     *  we would have defaulted to is described as the default. Documented rather than fixed:
     *  the description names the guard accurately either way, and the alternative is having the
     *  property object report whether a value was found, for no benefit a reader would notice.
     */
    public void testExplicitlyConfiguringTheDefaultGuardReadsAsTheDefault()
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                     ApparentlyLocalNameGuard.class.getName() );
        try
        {
            ReferenceableUtils.assertAcceptableName( "ldap://example.com", cfg );
            fail( "Expected NamingException: non-local name rejected" );
        }
        catch (NamingException e)
        {
            assertTrue( "Known, harmless: " + e.getMessage(),
                        e.getMessage().contains( "default NameGuard" ) );
        }
    }

    // ==========================================
    // referenceToObject – name-guard integration
    // referenceToObject calls assertAcceptableName when name != null.
    // ==========================================

    /** Non-null local name passes the default guard and dereferencing succeeds. */
    public void testReferenceToObjectWithLocalNameSucceeds() throws NamingException, InvalidNameException
    {
        String savedWl    = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        String savedGuard = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject(
                ref, new CompositeName( "java:comp/env/myDS" ), null, null );
            assertEquals( "ALPHA", result );
        }
        finally
        {
            restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, savedWl );
            restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, savedGuard );
        }
    }

    /** Non-null non-local name is rejected by the default guard → NamingException. */
    public void testReferenceToObjectWithNonLocalNameThrows() throws InvalidNameException
    {
        String savedWl    = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        String savedGuard = System.getProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            System.clearProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            ReferenceableUtils.referenceToObject(
                ref, new CompositeName( "ldap://example.com/myDS" ), null, null );
            fail( "Expected NamingException: non-local name rejected by default guard" );
        }
        catch (NamingException e) { /* expected */ }
        finally
        {
            restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, savedWl );
            restoreSystemProperty( SecurityConfigKey.NAME_GUARD_CLASS_NAME, savedGuard );
        }
    }

    /** Non-local name succeeds when AnyNameNameGuard is configured via pcfg. */
    public void testReferenceToObjectWithNonLocalNameAndAnyGuardSucceeds()
        throws NamingException, InvalidNameException
    {
        PropertiesConfig cfg = pcfg( SecurityConfigKey.NAME_GUARD_CLASS_NAME,
                                     AnyNameNameGuard.class.getName() );
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        Set whitelist = Collections.singleton( ALPHA_FACTORY );
        Object result = ReferenceableUtils.referenceToObject(
            ref, new CompositeName( "ldap://example.com/myDS" ), null, null, whitelist, cfg );
        assertEquals( "ALPHA", result );
    }

    // ==========================================
    // falseBiasedLookup logic (exercised via
    // supportReferenceRemoteFactoryClassLocation and
    // acceptDeserializedInitialContextEnvironment)
    // ==========================================

    // --- supportReferenceRemoteFactoryClassLocation ---

    public void testSupportRemoteFactoryDefaultFalse()
    { assertFalse( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation( null ) ); }

    public void testSupportRemoteFactoryPcfgTrue()
    {
        assertTrue( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation(
            pcfg( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, "true" ) ) );
    }

    public void testSupportRemoteFactoryPcfgFalse()
    {
        assertFalse( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation(
            pcfg( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, "false" ) ) );
    }

    public void testSupportRemoteFactorySyspropTruePcfgNull()
    {
        String saved = System.getProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION );
        try
        {
            System.setProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, "true" );
            assertTrue( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation( null ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, saved ); }
    }

    public void testSupportRemoteFactorySyspropFalsePcfgNull()
    {
        String saved = System.getProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION );
        try
        {
            System.setProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, "false" );
            assertFalse( ReferenceableUtils.supportReferenceRemoteFactoryClassLocation( null ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.SUPPORT_REFERENCE_REMOTE_FACTORY_CLASS_LOCATION, saved ); }
    }

    // --- acceptDeserializedInitialContextEnvironment ---

    public void testAcceptDeserializedDefaultFalse()
    { assertFalse( ReferenceableUtils.acceptDeserializedInitialContextEnvironment( null ) ); }

    public void testAcceptDeserializedPcfgTrue()
    {
        assertTrue( ReferenceableUtils.acceptDeserializedInitialContextEnvironment(
            pcfg( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, "true" ) ) );
    }

    public void testAcceptDeserializedPcfgFalse()
    {
        assertFalse( ReferenceableUtils.acceptDeserializedInitialContextEnvironment(
            pcfg( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, "false" ) ) );
    }

    public void testAcceptDeserializedSyspropTruePcfgNull()
    {
        String saved = System.getProperty( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT );
        try
        {
            System.setProperty( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, "true" );
            assertTrue( ReferenceableUtils.acceptDeserializedInitialContextEnvironment( null ) );
        }
        finally { restoreSystemProperty( SecurityConfigKey.ACCEPT_DESERIALIZED_INITIAL_CONTEXT_ENVIRONMENT, saved ); }
    }

    // ==========================================
    // referenceToObject – whitelist
    // ==========================================

    /** Explicit non-null whitelist containing the factory → succeeds */
    public void testReferenceToObjectExplicitWhitelist() throws NamingException
    {
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        Set whitelist = Collections.singleton( ALPHA_FACTORY );
        Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, whitelist );
        assertEquals( "ALPHA", result );
    }

    /** ALL_FACTORY_CLASS_NAMES token bypasses whitelist → any factory accepted */
    public void testReferenceToObjectAllFactoryClassNamesAcceptsAny() throws NamingException
    {
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, ReferenceableUtils.ALL_FACTORY_CLASS_NAMES );
        assertEquals( "ALPHA", result );
    }

    /** A regular empty Set (not the ALL_FACTORY_CLASS_NAMES token) rejects all factory class names */
    public void testReferenceToObjectRegularEmptySetRejectsAll()
    {
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        Set emptySet = Collections.unmodifiableSet( new HashSet() );
        try
        {
            ReferenceableUtils.referenceToObject( ref, null, null, null, emptySet );
            fail( "Expected NamingException: regular empty Set should reject all factory class names" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Factory not in explicit whitelist → NamingException */
    public void testReferenceToObjectFactoryNotInWhitelistThrows()
    {
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        Set whitelist = Collections.singleton( BETA_FACTORY );
        try
        {
            ReferenceableUtils.referenceToObject( ref, null, null, null, whitelist );
            fail( "Expected NamingException: factory not in whitelist" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Null factoryClassName → NamingException regardless of whitelist */
    public void testReferenceToObjectNullFactoryClassThrows()
    {
        Reference ref = new Reference( "java.lang.String", null, null );
        try
        {
            ReferenceableUtils.referenceToObject( ref, null, null, null, ReferenceableUtils.ALL_FACTORY_CLASS_NAMES );
            fail( "Expected NamingException: null factory class name" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Whitelist supplied via PropertiesConfig */
    public void testReferenceToObjectWhitelistViaPcfg() throws NamingException
    {
        Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
        PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
        Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, cfg );
        assertEquals( "ALPHA", result );
    }

    /** No pcfg, no sysprop whitelist → NamingException (mandatory whitelist missing) */
    public void testReferenceToObjectNoWhitelistConfiguredThrows()
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.clearProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            ReferenceableUtils.referenceToObject( ref, null, null, null, (PropertiesConfig) null );
            fail( "Expected NamingException: no whitelist configured" );
        }
        catch (NamingException e) { /* expected */ }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /** Whitelist supplied via system property */
    public void testReferenceToObjectWhitelistViaSysprop() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, (PropertiesConfig) null );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * When sysprop and pcfg whitelists differ, the intersection is used.
     * sysprop = {ALPHA, BETA}, pcfg = {BETA} → intersection = {BETA}
     * ALPHA factory should be rejected; BETA factory should be accepted.
     */
    public void testReferenceToObjectWhitelistIntersection() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY + "," + BETA_FACTORY );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, BETA_FACTORY );

            // ALPHA is only in sysprop list, not in intersection → rejected
            Reference refAlpha = makeRef( "java.lang.String", ALPHA_FACTORY );
            try
            {
                ReferenceableUtils.referenceToObject( refAlpha, null, null, null, cfg );
                fail( "Expected NamingException: ALPHA not in intersection whitelist" );
            }
            catch (NamingException e) { /* expected */ }

            // BETA is in both → accepted
            Reference refBeta = makeRef( "java.lang.String", BETA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( refBeta, null, null, null, cfg );
            assertEquals( "BETA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * When sysprop and pcfg have identical whitelists, the result is that set
     * (no intersection narrowing, no warning).
     */
    public void testReferenceToObjectWhitelistSyspropAndPcfgAgree() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, cfg );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /** No-pcfg no-set overload uses mandatory whitelist from sysprop when present */
    public void testReferenceToObjectNoArgsOverloadUsesSysprop() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    // ==========================================
    // referenceToObject – wildcard "*" ObjectFactory whitelist
    //
    // A sole-"*" ObjectFactory whitelist disables the per-factory restriction (any
    // ObjectFactory is accepted), under the same intentional rules as the JavaBean class
    // whitelist:
    //
    //   (a) "*" is the sole entry in System properties AND the PropertiesConfig, OR
    //   (b) "*" is the sole entry in one of those places and the OTHER place has no entry
    //       for the whitelist key at all.
    //
    // Pairing "*" with any other entry, in any location, must NOT disable the check -- in
    // particular, an intersection that merely narrows down to { "*" } leaves the check
    // enforced (and, since no real factory class equals "*", effectively rejects all).
    //
    // The ALPHA factory is never explicitly whitelisted in these tests, so its acceptance
    // is proof that the per-factory restriction is genuinely disabled.
    // ==========================================

    /** A non-null PropertiesConfig that carries some unrelated property but NOT the whitelist key. */
    private static PropertiesConfig pcfgWithoutWhitelist()
    { return pcfg( "com.mchange.v2.naming.someUnrelatedProperty", "irrelevant" ); }

    // ---- (b): "*" sole in one place, no entry in the other -> disabled ----

    /** "*" sole in pcfg, no whitelist sysprop -> any factory accepted. */
    public void testReferenceToObjectWildcardPcfgOnly() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.clearProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*" );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, cfg );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /** "*" sole in sysprop, no pcfg at all -> any factory accepted. */
    public void testReferenceToObjectWildcardSyspropNoPcfg() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*" );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * "*" sole in sysprop, with a pcfg that is present but carries no whitelist key.
     * Per intent (b) the check is disabled and any factory is accepted. (This is the case
     * that exposed the wrong-key / NPE bug in whitelistIsDisabled.)
     */
    public void testReferenceToObjectWildcardSyspropWithUnrelatedPcfg() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*" );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, pcfgWithoutWhitelist() );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    // ---- (a): "*" sole in BOTH places -> disabled ----

    /** "*" sole in BOTH sysprop and pcfg -> any factory accepted. */
    public void testReferenceToObjectWildcardBothPlaces() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*" );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*" );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            Object result = ReferenceableUtils.referenceToObject( ref, null, null, null, cfg );
            assertEquals( "ALPHA", result );
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    // ---- "*" paired with other entries, anywhere -> NOT disabled ----

    /**
     * "*" combined with another entry in a single source is NOT a wildcard. The "*" is a
     * literal (meaningless) factory class name: explicitly listed factories still pass, but
     * a factory covered only by the would-be wildcard is rejected.
     */
    public void testReferenceToObjectWildcardMixedWithOtherEntriesNotWildcard() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.clearProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*," + ALPHA_FACTORY );

            // ALPHA is explicitly present -> accepted
            Reference refAlpha = makeRef( "java.lang.String", ALPHA_FACTORY );
            assertEquals( "ALPHA", ReferenceableUtils.referenceToObject( refAlpha, null, null, null, cfg ) );

            // BETA is covered only by the (non-)wildcard -> rejected
            Reference refBeta = makeRef( "java.lang.String", BETA_FACTORY );
            try
            {
                ReferenceableUtils.referenceToObject( refBeta, null, null, null, cfg );
                fail( "Expected NamingException: '*' mixed with other entries must not act as a wildcard" );
            }
            catch (NamingException e) { /* expected */ }
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * "*" sole in sysprop, but pcfg pairs "*" with another entry. The post-intersection
     * whitelist is exactly { "*" }, yet the check must NOT be disabled because "*" is not
     * the sole entry in BOTH places. The (unlisted) ALPHA factory is rejected.
     */
    public void testReferenceToObjectWildcardSolePropPairedPcfgNotDisabled() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*" );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*," + BETA_FACTORY );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            try
            {
                ReferenceableUtils.referenceToObject( ref, null, null, null, cfg );
                fail( "Expected NamingException: '*' must be the SOLE entry in both places to disable the check" );
            }
            catch (NamingException e) { /* expected */ }
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * Two distinct multi-entry whitelists that share only "*" ("*,ALPHA" in sysprop,
     * "*,BETA" in pcfg) narrow by intersection to exactly { "*" } -- but this
     * intersection-derived "*" must NOT disable the check. The ALPHA factory is rejected,
     * and since no real factory class equals the literal "*", the effective whitelist
     * permits nothing. (This is the scenario the wrong-key bug got wrong.)
     */
    public void testReferenceToObjectWildcardIntersectionNarrowsToWildcardNotDisabled() throws NamingException
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*," + ALPHA_FACTORY );
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*," + BETA_FACTORY );
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            try
            {
                ReferenceableUtils.referenceToObject( ref, null, null, null, cfg );
                fail( "Expected NamingException: an intersection-derived '*' must not disable the check" );
            }
            catch (NamingException e) { /* expected */ }
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    /**
     * "*" in only ONE source with a restrictive list in the other: the intersection of
     * { "*" } and { ALPHA } is empty, which collapses to "no whitelist found" -> the
     * mandatory-whitelist check throws. A wildcard in one config cannot be smuggled past a
     * restrictive whitelist in another.
     */
    public void testReferenceToObjectWildcardInOnlyOneSourceDoesNotOpen()
    {
        String saved = System.getProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST );
        try
        {
            System.setProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, "*" );          // sysprop says "anything"
            PropertiesConfig cfg = pcfg( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, ALPHA_FACTORY ); // pcfg restricts
            Reference ref = makeRef( "java.lang.String", ALPHA_FACTORY );
            try
            {
                ReferenceableUtils.referenceToObject( ref, null, null, null, cfg );
                fail( "Expected NamingException: '*' in one source must not widen a restrictive whitelist in another" );
            }
            catch (NamingException e) { /* expected */ }
        }
        finally { restoreSystemProperty( SecurityConfigKey.OBJECT_FACTORY_WHITELIST, saved ); }
    }

    // ==========================================
    // appendToReference / extractNestedReference (deprecated)
    // ==========================================

    public void testAppendAndExtractSingleNestedReference() throws NamingException
    {
        Reference inner = new Reference( "com.example.Foo", "com.example.FooFactory", null );
        inner.add( new StringRefAddr( "key1", "value1" ) );
        inner.add( new StringRefAddr( "key2", "value2" ) );

        Reference outer = new Reference( "com.example.Bar" );
        ReferenceableUtils.appendToReference( outer, inner );

        ReferenceableUtils.ExtractRec rec = ReferenceableUtils.extractNestedReference( outer, 0 );
        Reference extracted = rec.ref;

        assertEquals( "com.example.Foo", extracted.getClassName() );
        assertEquals( "com.example.FooFactory", extracted.getFactoryClassName() );
        // factoryClassLocation was null; appendToReference stores the literal "null"
        assertNull( ReferenceableUtils.literalNullToNull( extracted.getFactoryClassLocation() ) );
        assertEquals( 2, extracted.size() );
        assertEquals( "value1", extracted.get( "key1" ).getContent() );
        assertEquals( "value2", extracted.get( "key2" ).getContent() );
        // rec.index should point past all the appended entries
        assertEquals( outer.size(), rec.index );
    }

    /** Append two references in sequence; verify each can be extracted at the correct index. */
    public void testAppendAndExtractMultipleNestedReferences() throws NamingException
    {
        Reference inner1 = new Reference( "com.example.Foo", "com.example.FooFactory", null );
        inner1.add( new StringRefAddr( "foo-key", "foo-val" ) );

        Reference inner2 = new Reference( "com.example.Bar", "com.example.BarFactory", null );
        inner2.add( new StringRefAddr( "bar-key", "bar-val" ) );

        Reference outer = new Reference( "com.example.Outer" );
        ReferenceableUtils.appendToReference( outer, inner1 );
        ReferenceableUtils.appendToReference( outer, inner2 );

        ReferenceableUtils.ExtractRec rec1 = ReferenceableUtils.extractNestedReference( outer, 0 );
        assertEquals( "com.example.Foo", rec1.ref.getClassName() );
        assertEquals( "foo-val", rec1.ref.get( "foo-key" ).getContent() );

        ReferenceableUtils.ExtractRec rec2 = ReferenceableUtils.extractNestedReference( outer, rec1.index );
        assertEquals( "com.example.Bar", rec2.ref.getClassName() );
        assertEquals( "bar-val", rec2.ref.get( "bar-key" ).getContent() );

        assertEquals( outer.size(), rec2.index );
    }

    /** A reference with no RefAddrs round-trips correctly. */
    public void testAppendAndExtractEmptyNestedReference() throws NamingException
    {
        Reference inner = new Reference( "com.example.Empty", "com.example.EmptyFactory", null );
        Reference outer = new Reference( "com.example.Outer" );
        ReferenceableUtils.appendToReference( outer, inner );

        ReferenceableUtils.ExtractRec rec = ReferenceableUtils.extractNestedReference( outer, 0 );
        assertEquals( "com.example.Empty", rec.ref.getClassName() );
        assertEquals( 0, rec.ref.size() );
        assertEquals( outer.size(), rec.index );
    }
}
