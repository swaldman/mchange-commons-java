package com.mchange.v2.naming.junit;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;
import javax.naming.Reference;

import junit.framework.TestCase;

import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.naming.JavaBeanObjectFactory;
import com.mchange.v2.naming.JavaBeanReferenceMaker;
import com.mchange.v2.naming.ReferenceableUtils;
import com.mchange.v2.naming.SecurityConfigKey;

/**
 *  That the two com.mchange.v2.naming whitelists are actually wired to
 *  WhitelistManager, rather than merely resolving as they always did.
 *
 *  <p>WhitelistManager's own behavior is covered in com.mchange.v2.cfg. What is untested
 *  without these is the wiring: that a name written under the <i>new</i> keys reaches
 *  findMandatoryObjectFactoryWhitelist and ensureWhitelistedJavaBeanClass at all, that the
 *  additive subkeys and the override key work there, that the sentinels mean what they mean
 *  everywhere else, and -- most importantly for anyone upgrading -- that the pre-existing
 *  flat keys still decide, loudly deprecated but undiminished.</p>
 *
 *  <p>The older ReferenceableUtils and JavaBeanReferenceable cases drive these gates through
 *  the deprecated keys, which is exactly right: they pin the compatibility this migration
 *  promised. These cover the other side of that promise.</p>
 */
public class NamingWhitelistWiringJUnitTestCase extends TestCase
{
    private final static String OF_BASE   = SecurityConfigKey.OBJECT_FACTORY_BASE_KEY;
    private final static String OF_WL     = OF_BASE + ".whitelist";
    private final static String OF_OVER   = OF_BASE + ".overrideWhitelist";
    private final static String OF_DEPR   = SecurityConfigKey.OBJECT_FACTORY_WHITELIST;

    private final static String JB_BASE   = SecurityConfigKey.REFERENCEABLE_JAVA_BEAN_CLASS_BASE_KEY;
    private final static String JB_WL     = JB_BASE + ".whitelist";
    private final static String JB_OVER   = JB_BASE + ".overrideWhitelist";
    private final static String JB_DEPR   = SecurityConfigKey.REFERENCEABLE_JAVA_BEAN_CLASS_WHITELIST;

    private final static String WILDCARD = "*";
    private final static String DENY_ALL = "[]";

    private final static String ALPHA_FACTORY = ReferenceableUtilsJUnitTestCase.AlphaObjectFactory.class.getName();

    /** a bean whose class name the JavaBean whitelist may or may not contain */
    public static class WiringBean
    {
        private String name = "unset";
        public String getName() { return name; }
        public void setName( String name ) { this.name = name; }
    }

    private final static String BEAN_FQCN = WiringBean.class.getName();

    private Properties saved;

    @Override
    public void setUp()
    {
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
    }

    @Override
    public void tearDown()
    {
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( ours( k ) )
                System.setProperty( k, saved.getProperty( k ) );
    }

    private static boolean ours( String k )
    { return k.startsWith( OF_BASE ) || k.startsWith( JB_BASE ); }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( ours( k ) )
                doomed.add( k );
        for ( String k : doomed )
            System.clearProperty( k );
    }

    private static PropertiesConfig pcfg( String... keysAndValues )
    {
        Properties p = new Properties();
        for ( int i = 0; i < keysAndValues.length; i += 2 )
            p.setProperty( keysAndValues[i], keysAndValues[i + 1] );
        return MultiPropertiesConfig.fromProperties( "/test", p );
    }

    // ---------- the ObjectFactory whitelist, via referenceToObject ----------

    private boolean factoryAccepted()
    { return factoryAccepted( null ); }

    private boolean factoryAccepted( PropertiesConfig pcfg )
    {
        Reference ref = new Reference( "java.lang.String", ALPHA_FACTORY, null );
        try
        {
            Object out = ( pcfg == null
                           ? ReferenceableUtils.referenceToObject( ref, null, null, null )
                           : ReferenceableUtils.referenceToObject( ref, null, null, null, pcfg ) );
            assertEquals( "Precondition: the test factory produces a known value.", "ALPHA", out );
            return true;
        }
        catch ( NamingException e )
        { return false; }
    }

    // ---------- the JavaBean whitelist, via createReference ----------

    private boolean beanAccepted()
    { return beanAccepted( null ); }

    private boolean beanAccepted( PropertiesConfig pcfg )
    {
        try
        {
            Reference ref = new JavaBeanReferenceMaker().createReference( new WiringBean(), pcfg );
            assertNotNull( ref );
            return true;
        }
        catch ( NamingException e )
        { return false; }
    }

    // ==================== ObjectFactory whitelist ====================

    /** The new base key's subkeys reach the gate at all -- the wiring this migration is. */
    public void testObjectFactoryNewSubkeyIsHonored()
    {
        System.setProperty( OF_WL + ".layerOne", ALPHA_FACTORY );

        assertTrue( "A factory named under the new subkeys must be accepted.", factoryAccepted() );
    }

    public void testObjectFactoryBareNewKeyIsHonored()
    {
        System.setProperty( OF_WL, ALPHA_FACTORY );

        assertTrue( factoryAccepted() );
    }

    /** and a factory absent from them is still refused, so the gate has not simply opened. */
    public void testObjectFactoryAbsentFromNewKeysIsRefused()
    {
        System.setProperty( OF_WL + ".layerOne", "com.example.SomeOtherFactory" );

        assertFalse( factoryAccepted() );
    }

    /** Subkeys union, so one layer's entry does not displace another's. */
    public void testObjectFactorySubkeysUnion()
    {
        System.setProperty( OF_WL + ".libraryDefaults", "com.example.SomeOtherFactory" );
        System.setProperty( OF_WL + ".deployment", ALPHA_FACTORY );

        assertTrue( "Both layers contribute.", factoryAccepted() );
    }

    public void testObjectFactoryOverrideReplacesTheSubkeys()
    {
        System.setProperty( OF_WL + ".layerOne", ALPHA_FACTORY );
        System.setProperty( OF_OVER, "com.example.SomeOtherFactory" );

        assertFalse( "An override discards what the subkeys contributed.", factoryAccepted() );

        System.setProperty( OF_OVER, ALPHA_FACTORY );
        assertTrue( factoryAccepted() );
    }

    /**
     *  The compatibility promise: an existing deployment still carrying the flat key keeps
     *  exactly the behavior it had, and the new keys do not quietly widen it.
     */
    public void testObjectFactoryDeprecatedKeyStillDecides()
    {
        System.setProperty( OF_DEPR, ALPHA_FACTORY );
        assertTrue( "The pre-existing flat key must still admit its factory.", factoryAccepted() );

        System.setProperty( OF_DEPR, "com.example.SomeOtherFactory" );
        System.setProperty( OF_WL + ".layerOne", ALPHA_FACTORY );
        assertFalse( "and must not be widened by the new keys while it is present.", factoryAccepted() );
    }

    public void testObjectFactoryWildcardAcceptsAnyFactory()
    {
        System.setProperty( OF_WL + ".layerOne", WILDCARD );

        assertTrue( factoryAccepted() );
    }

    /**
     *  The deny-all sentinel must not be mistaken for ALL_FACTORY_CLASS_NAMES, the empty Set
     *  that means accept-any. That token is distinguished by reference identity; a
     *  config-derived '[]' yields an ordinary empty Set, and confusing the two would invert
     *  a deny-all into accept-everything.
     */
    public void testObjectFactoryDenyAllSentinelRefusesRatherThanAcceptingAny()
    {
        System.setProperty( OF_WL + ".layerOne", ALPHA_FACTORY );
        System.setProperty( OF_WL + ".layerTwo", DENY_ALL );

        assertFalse( "'[]' must deny, not open the gate.", factoryAccepted() );
    }

    /** An unconfigured whitelist still demands configuration rather than silently denying. */
    public void testObjectFactoryUnconfiguredIsRefusedAndSaysSo()
    {
        Reference ref = new Reference( "java.lang.String", ALPHA_FACTORY, null );
        try
        {
            ReferenceableUtils.referenceToObject( ref, null, null, null );
            fail( "An unconfigured ObjectFactory whitelist must refuse." );
        }
        catch ( NamingException e )
        {
            String m = e.getMessage();
            assertNotNull( m );
            assertTrue( "The refusal must name the key to configure: " + m, m.contains( OF_BASE ) );
        }
    }

    /** The new keys are read from supplied configuration too, not only System properties. */
    public void testObjectFactoryNewKeysAreReadFromPropertiesConfig()
    {
        assertTrue( factoryAccepted( pcfg( OF_WL + ".layerOne", ALPHA_FACTORY ) ) );
        assertFalse( factoryAccepted( pcfg( OF_WL + ".layerOne", "com.example.SomeOtherFactory" ) ) );
    }

    // ==================== referenceable JavaBean class whitelist ====================

    public void testJavaBeanNewSubkeyIsHonored()
    {
        System.setProperty( JB_WL + ".layerOne", BEAN_FQCN );

        assertTrue( "A bean named under the new subkeys must be referenceable.", beanAccepted() );
    }

    public void testJavaBeanAbsentFromNewKeysIsRefused()
    {
        System.setProperty( JB_WL + ".layerOne", "com.example.SomeOtherBean" );

        assertFalse( beanAccepted() );
    }

    public void testJavaBeanSubkeysUnion()
    {
        System.setProperty( JB_WL + ".libraryDefaults", "com.example.SomeOtherBean" );
        System.setProperty( JB_WL + ".deployment", BEAN_FQCN );

        assertTrue( beanAccepted() );
    }

    public void testJavaBeanOverrideReplacesTheSubkeys()
    {
        System.setProperty( JB_WL + ".layerOne", BEAN_FQCN );
        System.setProperty( JB_OVER, "com.example.SomeOtherBean" );

        assertFalse( beanAccepted() );
    }

    public void testJavaBeanDeprecatedKeyStillDecides()
    {
        System.setProperty( JB_DEPR, BEAN_FQCN );
        assertTrue( "The pre-existing flat key must still admit its bean.", beanAccepted() );

        System.setProperty( JB_DEPR, "com.example.SomeOtherBean" );
        System.setProperty( JB_WL + ".layerOne", BEAN_FQCN );
        assertFalse( "and must not be widened by the new keys while it is present.", beanAccepted() );
    }

    public void testJavaBeanWildcardAcceptsAnyBean()
    {
        System.setProperty( JB_WL + ".layerOne", WILDCARD );

        assertTrue( beanAccepted() );
    }

    public void testJavaBeanDenyAllSentinelRefuses()
    {
        System.setProperty( JB_WL + ".layerOne", BEAN_FQCN );
        System.setProperty( JB_WL + ".layerTwo", DENY_ALL );

        assertFalse( "'[]' must deny.", beanAccepted() );
    }

    public void testJavaBeanNewKeysAreReadFromPropertiesConfig()
    {
        assertTrue( beanAccepted( pcfg( JB_WL + ".layerOne", BEAN_FQCN ) ) );
        assertFalse( beanAccepted( pcfg( JB_WL + ".layerOne", "com.example.SomeOtherBean" ) ) );
    }

    /**
     *  A missing whitelist and a configured empty one are different conditions, and the
     *  messages must say which. Collapsing them is what the old code did, and it told a
     *  deployment that had deliberately configured deny-all that no whitelist was set.
     */
    public void testJavaBeanMissingAndConfiguredEmptyGiveDifferentMessages()
    {
        String missingMsg = beanRefusalMessage();
        assertNotNull( "An unconfigured whitelist must refuse.", missingMsg );
        assertTrue( "and must say no whitelist is set: " + missingMsg,
                    missingMsg.contains( "No whitelist is set" ) );

        System.setProperty( JB_WL + ".layerOne", DENY_ALL );
        String emptyMsg = beanRefusalMessage();
        assertNotNull( "A configured deny-all must also refuse.", emptyMsg );
        assertFalse( "but must not claim the whitelist is unset: " + emptyMsg,
                     emptyMsg.contains( "No whitelist is set" ) );
        assertTrue( "It must be the ordinary not-on-the-whitelist refusal, since the whitelist " +
                    "was configured -- asserting only the absence of the missing-whitelist wording " +
                    "would also pass for the unreachable unexpected-source error: " + emptyMsg,
                    emptyMsg.contains( "does not contain referenced class" ) );
    }

    private String beanRefusalMessage()
    {
        try
        {
            new JavaBeanReferenceMaker().createReference( new WiringBean(), null );
            return null;
        }
        catch ( NamingException e )
        { return e.getMessage(); }
    }
}
