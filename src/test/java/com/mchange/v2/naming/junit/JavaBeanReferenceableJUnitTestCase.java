package com.mchange.v2.naming.junit;

import java.util.*;
import javax.naming.*;
import junit.framework.TestCase;
import com.mchange.v2.cfg.CurrentConfigFinder;
import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.naming.JavaBeanObjectFactory;
import com.mchange.v2.naming.JavaBeanReferenceMaker;
import com.mchange.v2.naming.SecurityConfigKey;

/**
 * Exercises the SecurityConfigKey.REFERENCEABLE_JAVA_BEAN_CLASS_WHITELIST gate on both
 * the encode side (JavaBeanReferenceMaker.createReference) and the decode side
 * (JavaBeanObjectFactory.getObjectInstance). Hard-bias: the operation must fail unless
 * the bean's FQCN is explicitly listed in the whitelist, supplied via either a
 * PropertiesConfig or the corresponding system property.
 */
public final class JavaBeanReferenceableJUnitTestCase extends TestCase
{
    // ==========================================
    // Test bean: must be public static so Class.forName + Introspector work
    // ==========================================

    public static final class TestBean
    {
        private String name;
        public String getName()              { return name; }
        public void   setName( String name ) { this.name = name; }
    }

    private static final String TEST_BEAN_FQCN = TestBean.class.getName();
    private static final String OTHER_FQCN     = "com.example.NotARealClass";
    private static final String ANOTHER_FQCN   = "com.example.AnotherClass";
    private static final String WL_KEY         = SecurityConfigKey.REFERENCEABLE_JAVA_BEAN_CLASS_WHITELIST;

    // ==========================================
    // Helpers
    // ==========================================

    private static PropertiesConfig pcfg( String key, String value )
    {
        Properties p = new Properties();
        p.setProperty( key, value );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    private static void restoreSystemProperty( String key, String savedValue )
    {
        if ( savedValue == null )
            System.clearProperty( key );
        else
            System.setProperty( key, savedValue );
    }

    private static CurrentConfigFinder finderFor( final PropertiesConfig pcfg )
    {
        return new CurrentConfigFinder()
        {
            public PropertiesConfig findCurrentConfig() { return pcfg; }
        };
    }

    private static Reference referenceToTestBean()
    { return new Reference( TEST_BEAN_FQCN, JavaBeanObjectFactory.class.getName(), null ); }

    // ==========================================
    // Clear the whitelist sysprop before each test; individual tests
    // override it as they like. tearDown() restores the original value.
    // ==========================================

    private String savedWhitelistSysprop;

    protected void setUp()
    {
        savedWhitelistSysprop = System.getProperty( WL_KEY );
        System.clearProperty( WL_KEY );
    }

    protected void tearDown()
    { restoreSystemProperty( WL_KEY, savedWhitelistSysprop ); }

    // ==========================================
    // Encode side: JavaBeanReferenceMaker.createReference
    // ==========================================

    /** No whitelist anywhere → hard-bias rejects. */
    public void testCreateReferenceForbiddenWhenNoWhitelist()
    {
        JavaBeanReferenceMaker maker = new JavaBeanReferenceMaker();
        try
        {
            maker.createReference( new TestBean(), null );
            fail( "Expected NamingException with no whitelist set" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Whitelist present but doesn't list the bean class → reject. */
    public void testCreateReferenceForbiddenWhenClassNotInWhitelist()
    {
        JavaBeanReferenceMaker maker = new JavaBeanReferenceMaker();
        PropertiesConfig cfg = pcfg( WL_KEY, OTHER_FQCN );
        try
        {
            maker.createReference( new TestBean(), cfg );
            fail( "Expected NamingException when class not in whitelist" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Whitelist provided via pcfg and contains the bean class → succeed. */
    public void testCreateReferenceAllowedByPcfgWhitelist() throws Exception
    {
        JavaBeanReferenceMaker maker = new JavaBeanReferenceMaker();
        PropertiesConfig cfg = pcfg( WL_KEY, TEST_BEAN_FQCN );
        Reference ref = maker.createReference( new TestBean(), cfg );
        assertNotNull( ref );
        assertEquals( TEST_BEAN_FQCN, ref.getClassName() );
    }

    /** Whitelist provided via system property and contains the bean class → succeed. */
    public void testCreateReferenceAllowedBySyspropWhitelist() throws Exception
    {
        System.setProperty( WL_KEY, TEST_BEAN_FQCN );
        JavaBeanReferenceMaker maker = new JavaBeanReferenceMaker();
        Reference ref = maker.createReference( new TestBean(), null );
        assertNotNull( ref );
        assertEquals( TEST_BEAN_FQCN, ref.getClassName() );
    }

    // ==========================================
    // Decode side: JavaBeanObjectFactory.getObjectInstance
    // ==========================================

    /** No whitelist anywhere → hard-bias rejects on decode. */
    public void testGetObjectInstanceForbiddenWhenNoWhitelist() throws Exception
    {
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        try
        {
            factory.getObjectInstance( referenceToTestBean(), null, null, null );
            fail( "Expected NamingException with no whitelist set" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Whitelist visible to the factory via its CurrentConfigFinder but doesn't list the bean class → reject. */
    public void testGetObjectInstanceForbiddenWhenClassNotInWhitelist() throws Exception
    {
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        factory.setConfigFinder( finderFor( pcfg( WL_KEY, OTHER_FQCN ) ) );
        try
        {
            factory.getObjectInstance( referenceToTestBean(), null, null, null );
            fail( "Expected NamingException when class not in whitelist" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /** Whitelist via CurrentConfigFinder contains the class → succeed; the returned object is a TestBean. */
    public void testGetObjectInstanceAllowedByCfgFinder() throws Exception
    {
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        factory.setConfigFinder( finderFor( pcfg( WL_KEY, TEST_BEAN_FQCN ) ) );
        Object out = factory.getObjectInstance( referenceToTestBean(), null, null, null );
        assertNotNull( out );
        assertEquals( TestBean.class, out.getClass() );
    }

    /** With no CurrentConfigFinder set on the factory (the JNDI-default case), a sysprop whitelist still works. */
    public void testGetObjectInstanceAllowedBySysprop() throws Exception
    {
        System.setProperty( WL_KEY, TEST_BEAN_FQCN );
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        // cfgFinder deliberately unset, mirroring the JNDI no-arg-ctor case
        Object out = factory.getObjectInstance( referenceToTestBean(), null, null, null );
        assertNotNull( out );
        assertEquals( TestBean.class, out.getClass() );
    }

    /**
     * Defense-in-depth: a Reference carrying a bogus className must be rejected by the
     * whitelist BEFORE Class.forName runs. If the gate ran after Class.forName, we would
     * get a ClassNotFoundException instead of a NamingException.
     */
    public void testGetObjectInstanceRejectsClassnameBeforeLoading() throws Exception
    {
        Reference bogus = new Reference(
            "com.example.DoesNotExistAnywhere.MaliciousClass",
            JavaBeanObjectFactory.class.getName(),
            null
        );
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        try
        {
            factory.getObjectInstance( bogus, null, null, null );
            fail( "Expected NamingException for unwhitelisted bogus class without attempting to load it" );
        }
        catch (NamingException e) { /* expected: gate fired before Class.forName */ }
        catch (ClassNotFoundException e)
        { fail( "Whitelist gate must run BEFORE Class.forName; got ClassNotFoundException instead of NamingException" ); }
    }

    // ==========================================
    // Intersection / narrowing semantics
    // ==========================================

    /**
     * Whitelist in both sysprop AND pcfg, with the bean class in the intersection → succeed.
     * Verifies the narrowing helper takes the intersection rather than the union.
     */
    public void testWhitelistIntersectionContainsClass() throws Exception
    {
        System.setProperty( WL_KEY, TEST_BEAN_FQCN + "," + OTHER_FQCN );
        PropertiesConfig cfg = pcfg( WL_KEY, TEST_BEAN_FQCN + "," + ANOTHER_FQCN );
        JavaBeanReferenceMaker maker = new JavaBeanReferenceMaker();
        Reference ref = maker.createReference( new TestBean(), cfg );
        assertNotNull( ref );
    }

    /**
     * Whitelist in both sysprop AND pcfg, but the bean class is in only one of them → reject.
     * Confirms a sysprop whitelist cannot be silently widened by adding to the pcfg.
     */
    public void testWhitelistIntersectionExcludesClass()
    {
        System.setProperty( WL_KEY, TEST_BEAN_FQCN );  // sysprop includes the class
        PropertiesConfig cfg = pcfg( WL_KEY, OTHER_FQCN ); // pcfg does not
        JavaBeanReferenceMaker maker = new JavaBeanReferenceMaker();
        try
        {
            maker.createReference( new TestBean(), cfg );
            fail( "Expected NamingException: TestBean not in narrowed (intersection) whitelist" );
        }
        catch (NamingException e) { /* expected */ }
    }
}
