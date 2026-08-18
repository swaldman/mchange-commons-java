package com.mchange.v2.cfg;

import junit.framework.TestCase;

/**
 *  Lives in com.mchange.v2.cfg rather than com.mchange.v2.cfg.junit because it exercises
 *  package-private internals that the public surface does not expose.
 */
public final class BasicMultiPropertiesConfigInternalJUnitTestCase extends TestCase
{
    /**
     *  The EMPTY singleton's constructor once assigned to local variables that shadowed
     *  the fields, leaving propsByPrefixes, parseMessages, and propsByKey null -- so every
     *  accessor on it threw NullPointerException. It must be fully usable.
     */
    public void testEmptySingletonIsFullyInitialized()
    {
        BasicMultiPropertiesConfig empty = BasicMultiPropertiesConfig.EMPTY;

        assertEquals( 0, empty.getPropertiesResourcePaths().length );
        assertNull( empty.getProperty( "anything" ) );
        assertEquals( 0, empty.getPropertiesByPrefix( "" ).size() );
        assertEquals( 0, empty.getPropertiesByPrefix( "some.prefix" ).size() );
        assertEquals( 0, empty.getPropertiesByResourcePath( "/anything" ).size() );
        assertNotNull( empty.getDelayedLogItems() );
        assertEquals( 0, empty.getDelayedLogItems().size() );
    }

    /** isHoconPath is the shared predicate; ConfigUtils and configSource must agree. */
    public void testIsHoconPathPredicate()
    {
        assertTrue( BasicMultiPropertiesConfig.isHoconPath( "hocon:/reference" ) );
        assertTrue( "the prefix is case-insensitive",
                    BasicMultiPropertiesConfig.isHoconPath( "HOCON:/reference" ) );
        assertTrue( BasicMultiPropertiesConfig.isHoconPath( "hocon:/a,/b,/" ) );

        assertFalse( "a bare prefix names no elements",
                     BasicMultiPropertiesConfig.isHoconPath( "hocon:" ) );
        assertFalse( BasicMultiPropertiesConfig.isHoconPath( "/plain.properties" ) );
        assertFalse( BasicMultiPropertiesConfig.isHoconPath( "/" ) );
        assertFalse( BasicMultiPropertiesConfig.isHoconPath( "hoc" ) );
    }
}
