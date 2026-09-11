package com.mchange.v2.cfg;

import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

/**
 *  getPropertiesByPrefix(pfx) must include a key that <em>is</em> the prefix, not only keys
 *  beneath it.
 *
 *  <p>This became load-bearing with the by-name instantiation whitelist. That whitelist is
 *  gathered by asking for every property under a prefix and unioning the values, so a
 *  deployment that sets the bare key rather than a subkey -- the obvious thing to do when
 *  you have only one list to contribute -- would contribute nothing at all if the bare key
 *  were excluded.</p>
 *
 *  <p>Lives in com.mchange.v2.cfg rather than a junit subpackage because
 *  BasicMultiPropertiesConfig is not public.</p>
 */
public class PropertiesByPrefixInternalJUnitTestCase extends TestCase
{
    private final static String PFX  = "com.mchange.test.whitelist";
    private final static String BARE = PFX;
    private final static String SUB  = PFX + ".layerOne";

    private PropertiesConfig config( Properties props )
    { return new BasicMultiPropertiesConfig( "/notional-test-resource", props ); }

    private Properties props( String... keysAndValues )
    {
        Properties p = new Properties();
        for ( int i = 0; i < keysAndValues.length; i += 2 )
            p.setProperty( keysAndValues[i], keysAndValues[i+1] );
        return p;
    }

    /** The case the whitelist depends on. */
    public void testBarePrefixKeyIsIncludedUnderItsOwnPrefix()
    {
        PropertiesConfig cfg = config( props( BARE, "com.example.Alpha" ) );

        Properties byPfx = cfg.getPropertiesByPrefix( PFX );
        assertTrue( "A key identical to the prefix must appear under that prefix.",
                    byPfx.stringPropertyNames().contains( BARE ) );
        assertEquals( "com.example.Alpha", byPfx.getProperty( BARE ) );
    }

    /** and it must not displace keys beneath the prefix, nor they it. */
    public void testBarePrefixAndSubkeysCoexist()
    {
        PropertiesConfig cfg = config( props( BARE, "com.example.Alpha",
                                              SUB,  "com.example.Beta" ) );

        Set<String> names = cfg.getPropertiesByPrefix( PFX ).stringPropertyNames();
        assertEquals( 2, names.size() );
        assertTrue( names.contains( BARE ) );
        assertTrue( names.contains( SUB ) );
    }

    /** Keys come back whole, not stripped -- the whitelist re-reads each one by name. */
    public void testKeysAreReportedUnstripped()
    {
        PropertiesConfig cfg = config( props( SUB, "com.example.Beta" ) );

        Set<String> names = cfg.getPropertiesByPrefix( PFX ).stringPropertyNames();
        assertTrue( "Keys must come back fully qualified, not relative to the prefix.",
                    names.contains( SUB ) );
        assertFalse( names.contains( "layerOne" ) );
        assertEquals( "com.example.Beta", cfg.getProperty( SUB ) );
    }

    /** Every intermediate prefix indexes the key, so a shorter prefix still finds it. */
    public void testIntermediatePrefixesAlsoIndexTheKey()
    {
        PropertiesConfig cfg = config( props( SUB, "com.example.Beta" ) );

        assertTrue( cfg.getPropertiesByPrefix( "com.mchange.test" ).stringPropertyNames().contains( SUB ) );
        assertTrue( cfg.getPropertiesByPrefix( "com.mchange" ).stringPropertyNames().contains( SUB ) );
        assertTrue( cfg.getPropertiesByPrefix( "com" ).stringPropertyNames().contains( SUB ) );
    }

    /** "" is the prefix of everything, which is what the change-detection fingerprint relies on. */
    public void testEmptyPrefixReturnsEverything()
    {
        PropertiesConfig cfg = config( props( BARE, "a", SUB, "b", "unrelated.key", "c" ) );

        Set<String> names = cfg.getPropertiesByPrefix( "" ).stringPropertyNames();
        assertTrue( names.contains( BARE ) );
        assertTrue( names.contains( SUB ) );
        assertTrue( names.contains( "unrelated.key" ) );
    }

    /**
     *  A prefix must be a complete dot-separated token, as PropertiesConfig documents. A
     *  partial token matches nothing, which is why the whitelist prefix carries no trailing
     *  dot and is never truncated.
     */
    public void testPartialTokenIsNotAPrefix()
    {
        PropertiesConfig cfg = config( props( SUB, "com.example.Beta" ) );

        assertTrue( "Precondition: the complete token does match.",
                    cfg.getPropertiesByPrefix( PFX ).stringPropertyNames().contains( SUB ) );
        assertTrue( "A partial final token must not match as a prefix.",
                    cfg.getPropertiesByPrefix( "com.mchange.test.white" ).stringPropertyNames().isEmpty() );
    }

    public void testUnrelatedKeysAreExcluded()
    {
        PropertiesConfig cfg = config( props( SUB, "a", "com.mchange.other.thing", "b" ) );

        Set<String> names = cfg.getPropertiesByPrefix( PFX ).stringPropertyNames();
        assertEquals( 1, names.size() );
        assertTrue( names.contains( SUB ) );
    }
}
