package com.mchange.v2.beans.junit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import com.mchange.v2.beans.BeansUtils;

/**
 *  overwriteAccessiblePropertiesFromMap follows a java.util.Properties defaults chain.
 *
 *  <p>It used to read the source map through keySet(), and Properties.keySet() reports only
 *  the entries held directly -- a Properties constructed with defaults answers getProperty()
 *  for names that its keySet() never mentions. So every defaulted property was silently
 *  dropped, and the caller saw a bean missing values their Properties would happily have
 *  told them about.</p>
 *
 *  <p>The subtlety in the fix is that stringPropertyNames(), which does walk the chain,
 *  yields only entries whose key <i>and</i> value are Strings. Reading through it alone
 *  would lose direct entries that a Properties is perfectly able to hold -- a non-String
 *  value, or a non-String key -- so the direct entries have to be replayed over the top.
 *  Several tests below exist to keep both halves of that in place.</p>
 */
public class BeansUtilsPropertiesDefaultsJUnitTestCase extends TestCase
{
    public static class TestBean
    {
        private String alpha;
        private String beta;
        private String gamma;

        public String getAlpha()             { return alpha; }
        public void   setAlpha(String alpha) { this.alpha = alpha; }
        public String getBeta()              { return beta; }
        public void   setBeta(String beta)   { this.beta = beta; }
        public String getGamma()             { return gamma; }
        public void   setGamma(String gamma) { this.gamma = gamma; }
    }

    /** A bean with a non-String property, to check what survives of direct entries. */
    public static class IntBean
    {
        private Integer count;
        public Integer getCount()               { return count; }
        public void    setCount(Integer count)  { this.count = count; }
    }

    // ---------- the defaults chain ----------

    /** The case that was broken: a property present only in the defaults. */
    public void testPropertyFromDefaultsIsApplied() throws Exception
    {
        Properties defaults = new Properties();
        defaults.setProperty( "alpha", "from-defaults" );

        Properties props = new Properties( defaults );
        props.setProperty( "beta", "from-direct" );

        assertFalse( "Precondition: keySet() does not mention the defaulted name.",
                     props.keySet().contains( "alpha" ) );
        assertEquals( "Precondition: but getProperty finds it.", "from-defaults", props.getProperty( "alpha" ) );

        TestBean bean = new TestBean();
        BeansUtils.overwriteAccessiblePropertiesFromMap( props, bean, false );

        assertEquals( "A property held only in the defaults must be applied.", "from-defaults", bean.getAlpha() );
        assertEquals( "from-direct", bean.getBeta() );
    }

    /** A direct entry must beat a default of the same name, as Properties.getProperty does. */
    public void testDirectEntryOverridesDefault() throws Exception
    {
        Properties defaults = new Properties();
        defaults.setProperty( "alpha", "from-defaults" );
        defaults.setProperty( "beta",  "default-beta" );

        Properties props = new Properties( defaults );
        props.setProperty( "beta", "direct-beta" );

        TestBean bean = new TestBean();
        BeansUtils.overwriteAccessiblePropertiesFromMap( props, bean, false );

        assertEquals( "from-defaults", bean.getAlpha() );
        assertEquals( "A direct entry must win over a default of the same name.", "direct-beta", bean.getBeta() );
    }

    /** Properties chains can nest arbitrarily; the whole chain must be followed. */
    public void testNestedDefaultsChain() throws Exception
    {
        Properties grandparent = new Properties();
        grandparent.setProperty( "alpha", "from-grandparent" );

        Properties parent = new Properties( grandparent );
        parent.setProperty( "beta", "from-parent" );

        Properties props = new Properties( parent );
        props.setProperty( "gamma", "from-direct" );

        TestBean bean = new TestBean();
        BeansUtils.overwriteAccessiblePropertiesFromMap( props, bean, false );

        assertEquals( "from-grandparent", bean.getAlpha() );
        assertEquals( "from-parent",      bean.getBeta() );
        assertEquals( "from-direct",      bean.getGamma() );
    }

    public void testEmptyDefaultsChangesNothing() throws Exception
    {
        Properties props = new Properties( new Properties() );
        props.setProperty( "alpha", "direct" );

        TestBean bean = new TestBean();
        BeansUtils.overwriteAccessiblePropertiesFromMap( props, bean, false );

        assertEquals( "direct", bean.getAlpha() );
        assertNull( bean.getBeta() );
    }

    // ---------- what must NOT be lost ----------

    /**
     *  stringPropertyNames() omits any entry whose value is not a String, so reading only
     *  through it would drop direct entries a Properties can legitimately hold. Those must
     *  still reach the bean.
     */
    public void testDirectNonStringValueSurvives() throws Exception
    {
        Properties props = new Properties( new Properties() );
        props.put( "count", Integer.valueOf( 42 ) );

        assertFalse( "Precondition: stringPropertyNames omits a non-String value.",
                     props.stringPropertyNames().contains( "count" ) );

        IntBean bean = new IntBean();
        BeansUtils.overwriteAccessiblePropertiesFromMap( props, bean, false );

        assertEquals( "A direct non-String entry must still be applied.", Integer.valueOf( 42 ), bean.getCount() );
    }

    /**
     *  and a non-String key is likewise invisible to stringPropertyNames. Nothing can be
     *  set from it, but its presence must not disturb the properties that can be.
     */
    public void testDirectNonStringKeyDoesNotDisturbTheRest() throws Exception
    {
        Properties defaults = new Properties();
        defaults.setProperty( "alpha", "from-defaults" );

        Properties props = new Properties( defaults );
        props.put( Integer.valueOf( 1 ), "unreachable-by-name" );
        props.setProperty( "beta", "from-direct" );

        TestBean bean = new TestBean();
        BeansUtils.overwriteAccessiblePropertiesFromMap( props, bean, false );

        assertEquals( "from-defaults", bean.getAlpha() );
        assertEquals( "from-direct",   bean.getBeta() );
    }

    // ---------- interaction with the other arguments ----------

    /** ignoreProps must apply to a defaulted property exactly as to a direct one. */
    public void testIgnorePropsAppliesToDefaultedProperties() throws Exception
    {
        Properties defaults = new Properties();
        defaults.setProperty( "alpha", "from-defaults" );

        Properties props = new Properties( defaults );
        props.setProperty( "beta", "from-direct" );

        TestBean bean = new TestBean();
        BeansUtils.overwriteAccessiblePropertiesFromMap( props, bean, false, Arrays.asList( "alpha" ) );

        assertNull( "An ignored property must not be set even when it comes from the defaults.", bean.getAlpha() );
        assertEquals( "from-direct", bean.getBeta() );
    }

    // ---------- non-Properties maps are untouched ----------

    /** An ordinary Map takes the plain path; nothing about it should have changed. */
    public void testPlainMapIsUnaffected() throws Exception
    {
        Map<String,Object> m = new HashMap<String,Object>();
        m.put( "alpha", "a" );
        m.put( "beta",  "b" );

        TestBean bean = new TestBean();
        BeansUtils.overwriteAccessiblePropertiesFromMap( m, bean, false );

        assertEquals( "a", bean.getAlpha() );
        assertEquals( "b", bean.getBeta() );
        assertNull( bean.getGamma() );
    }

    /**
     *  The source map must not be modified. The defaults are merged into a copy, so a
     *  caller's Properties comes back exactly as they passed it -- in particular the
     *  defaulted names must not have been flattened into it.
     */
    public void testSourcePropertiesAreNotModified() throws Exception
    {
        Properties defaults = new Properties();
        defaults.setProperty( "alpha", "from-defaults" );

        Properties props = new Properties( defaults );
        props.setProperty( "beta", "from-direct" );

        BeansUtils.overwriteAccessiblePropertiesFromMap( props, new TestBean(), false );

        assertEquals( "The caller's Properties must not have gained the defaulted name.",
                      1, props.keySet().size() );
        assertTrue( props.keySet().contains( "beta" ) );
        assertFalse( props.keySet().contains( "alpha" ) );
    }
}
