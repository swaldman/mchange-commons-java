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

    // A second, real, dereferenceable bean that is never explicitly whitelisted by name in
    // these tests. Used to prove that a wildcard "*" whitelist opens dereferencing of an
    // arbitrary class -- one that would otherwise be rejected.
    public static final class AnotherTestBean
    {
        private int value;
        public int  getValue()             { return value; }
        public void setValue( int value )  { this.value = value; }
    }

    private static final String TEST_BEAN_FQCN    = TestBean.class.getName();
    private static final String ANOTHER_BEAN_FQCN = AnotherTestBean.class.getName();
    private static final String OTHER_FQCN        = "com.example.NotARealClass";
    private static final String ANOTHER_FQCN      = "com.example.AnotherClass";
    private static final String WILDCARD          = "*";
    private static final String WL_KEY            = SecurityConfigKey.REFERENCEABLE_JAVA_BEAN_CLASS_WHITELIST;

    // ==========================================
    // Helpers
    // ==========================================

    private static PropertiesConfig pcfg( String key, String value )
    {
        Properties p = new Properties();
        p.setProperty( key, value );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    /** A non-null PropertiesConfig that carries some unrelated property but NOT the whitelist key. */
    private static PropertiesConfig pcfgWithoutWhitelist()
    { return pcfg( "com.mchange.v2.naming.someUnrelatedProperty", "irrelevant" ); }

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

    private static Reference referenceTo( String fqcn )
    { return new Reference( fqcn, JavaBeanObjectFactory.class.getName(), null ); }

    private static Reference referenceToTestBean()
    { return referenceTo( TEST_BEAN_FQCN ); }

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

    // ==========================================
    // Wildcard "*" semantics
    //
    // The whitelist check is disabled (any JavaBean class accepted) ONLY when "*" is very
    // intentionally specified as the SOLE whitelist entry:
    //
    //   (a) "*" is the sole entry in System properties AND the PropertiesConfig, OR
    //   (b) "*" is the sole entry in one of those places and the OTHER place has no entry
    //       for the whitelist key at all.
    //
    // Pairing "*" with any other entry, in any location, must NOT disable the check.
    // In particular, the check must NOT be disabled merely because the post-intersection
    // whitelist happens to narrow down to { "*" } (e.g. "*,X" intersected with "*,Y"):
    // such an intersection-derived "*" leaves the check fully enforced (and, since no real
    // class equals "*", effectively rejects everything).
    // ==========================================

    // ---- (b): "*" sole in one place, no entry in the other -> disabled ----

    /**
     * "*" is the sole whitelist entry in the PropertiesConfig and there is no whitelist
     * system property (cleared in setUp). The decode side opens to an arbitrary class:
     * AnotherTestBean dereferences successfully despite never being listed by name.
     */
    public void testGetObjectInstanceAllowedByWildcardPcfgOnly() throws Exception
    {
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        factory.setConfigFinder( finderFor( pcfg( WL_KEY, WILDCARD ) ) );
        Object out = factory.getObjectInstance( referenceTo( ANOTHER_BEAN_FQCN ), null, null, null );
        assertNotNull( out );
        assertEquals( AnotherTestBean.class, out.getClass() );
    }

    /**
     * "*" is the sole whitelist entry in System properties and there is no PropertiesConfig
     * at all (the JNDI-default, no-CurrentConfigFinder case). The decode side opens to an
     * arbitrary class.
     */
    public void testGetObjectInstanceAllowedByWildcardSyspropNoPcfg() throws Exception
    {
        System.setProperty( WL_KEY, WILDCARD );
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        // cfgFinder deliberately unset -> pcfg is null
        Object out = factory.getObjectInstance( referenceTo( ANOTHER_BEAN_FQCN ), null, null, null );
        assertNotNull( out );
        assertEquals( AnotherTestBean.class, out.getClass() );
    }

    /**
     * "*" is the sole whitelist entry in System properties, and a PropertiesConfig IS
     * present but carries no entry for the whitelist key. Per intent (b) this disables the
     * check and opens dereferencing of an arbitrary class.
     */
    public void testGetObjectInstanceAllowedByWildcardSyspropWithUnrelatedPcfg() throws Exception
    {
        System.setProperty( WL_KEY, WILDCARD );
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        factory.setConfigFinder( finderFor( pcfgWithoutWhitelist() ) ); // present, but no whitelist key
        Object out = factory.getObjectInstance( referenceTo( ANOTHER_BEAN_FQCN ), null, null, null );
        assertNotNull( out );
        assertEquals( AnotherTestBean.class, out.getClass() );
    }

    // ---- (a): "*" sole in BOTH places -> disabled ----

    /**
     * "*" is the sole whitelist entry in BOTH System properties and the PropertiesConfig.
     * The check is disabled and an arbitrary class dereferences.
     */
    public void testGetObjectInstanceAllowedByWildcardBothPlaces() throws Exception
    {
        System.setProperty( WL_KEY, WILDCARD );
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        factory.setConfigFinder( finderFor( pcfg( WL_KEY, WILDCARD ) ) );
        Object out = factory.getObjectInstance( referenceTo( ANOTHER_BEAN_FQCN ), null, null, null );
        assertNotNull( out );
        assertEquals( AnotherTestBean.class, out.getClass() );
    }

    /** For symmetry: a sole-"*" whitelist also opens the encode side to an arbitrary class. */
    public void testCreateReferenceAllowedByWildcard() throws Exception
    {
        JavaBeanReferenceMaker maker = new JavaBeanReferenceMaker();
        Reference ref = maker.createReference( new AnotherTestBean(), pcfg( WL_KEY, WILDCARD ) );
        assertNotNull( ref );
        assertEquals( ANOTHER_BEAN_FQCN, ref.getClassName() );
    }

    // ---- "*" paired with other entries, anywhere -> NOT disabled ----

    /**
     * "*" combined with other entries in a single source is NOT a wildcard. The "*" is
     * treated as a literal (and meaningless) class name: explicitly listed classes still
     * pass, but a class covered only by the would-be wildcard is rejected.
     */
    public void testWildcardMixedWithOtherEntriesIsNotWildcard() throws Exception
    {
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        factory.setConfigFinder( finderFor( pcfg( WL_KEY, WILDCARD + "," + TEST_BEAN_FQCN ) ) );

        // TestBean is explicitly present -> allowed
        Object out = factory.getObjectInstance( referenceToTestBean(), null, null, null );
        assertNotNull( out );
        assertEquals( TestBean.class, out.getClass() );

        // AnotherTestBean is covered only by the (non-)wildcard -> rejected
        try
        {
            factory.getObjectInstance( referenceTo( ANOTHER_BEAN_FQCN ), null, null, null );
            fail( "Expected NamingException: '*' mixed with other entries must not act as a wildcard" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /**
     * "*" is the sole entry in System properties, but the PropertiesConfig pairs "*" with
     * another entry. Even though the post-intersection whitelist is exactly { "*" }, the
     * check must NOT be disabled, because "*" is not the sole entry in BOTH places. An
     * arbitrary class is rejected.
     */
    public void testWildcardSolePropOtherPairedPcfgNotDisabled() throws Exception
    {
        System.setProperty( WL_KEY, WILDCARD );
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        factory.setConfigFinder( finderFor( pcfg( WL_KEY, WILDCARD + "," + ANOTHER_FQCN ) ) );
        try
        {
            factory.getObjectInstance( referenceTo( ANOTHER_BEAN_FQCN ), null, null, null );
            fail( "Expected NamingException: '*' must be the SOLE entry in both places to disable the check" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /**
     * Two distinct multi-entry whitelists that share only "*" ("*,OTHER" in sysprop,
     * "*,ANOTHER" in pcfg) narrow by intersection to exactly { "*" } -- but this
     * intersection-derived "*" must NOT disable the check. An arbitrary class is rejected,
     * and since no real class equals the literal "*", the effective whitelist permits
     * nothing.
     */
    public void testWildcardIntersectionNarrowsToWildcardStillNotDisabled() throws Exception
    {
        System.setProperty( WL_KEY, WILDCARD + "," + OTHER_FQCN );
        JavaBeanObjectFactory factory = new JavaBeanObjectFactory();
        factory.setConfigFinder( finderFor( pcfg( WL_KEY, WILDCARD + "," + ANOTHER_FQCN ) ) );
        try
        {
            factory.getObjectInstance( referenceTo( ANOTHER_BEAN_FQCN ), null, null, null );
            fail( "Expected NamingException: an intersection-derived '*' must not disable the check" );
        }
        catch (NamingException e) { /* expected */ }
    }

    /**
     * "*" in only ONE source with a restrictive class list in the other: the intersection
     * of { "*" } and { TestBean } is empty, which collapses to "no whitelist" -- so even
     * TestBean (present in the sysprop source) is rejected. A wildcard in one config cannot
     * be smuggled in past a restrictive whitelist in another.
     */
    public void testWildcardInOnlyOneSourceDoesNotOpen()
    {
        System.setProperty( WL_KEY, WILDCARD );             // sysprop says "anything"
        PropertiesConfig cfg = pcfg( WL_KEY, TEST_BEAN_FQCN ); // pcfg restricts to TestBean
        JavaBeanReferenceMaker maker = new JavaBeanReferenceMaker();
        try
        {
            maker.createReference( new TestBean(), cfg );
            fail( "Expected NamingException: '*' in one source must not widen a restrictive whitelist in another" );
        }
        catch (NamingException e) { /* expected */ }
    }
}
