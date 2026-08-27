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
     *
     *  <p>Every accessor is exercised because the failure mode is per-field: the constructor
     *  has to assign each one by hand, and the history sets brought the count to eight. A field
     *  left out here does not fail at construction, only later, on whichever call touches it.</p>
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

        assertNotNull( empty.getAllRead() );
        assertNotNull( empty.getAllVetoed() );
        assertNotNull( empty.getAllNotFound() );
        assertNotNull( empty.getAllFaults() );
        assertEquals( 0, empty.getAllRead().size() );
        assertEquals( 0, empty.getAllVetoed().size() );
        assertEquals( 0, empty.getAllNotFound().size() );
        assertEquals( 0, empty.getAllFaults().size() );

        assertFalse( empty.wasEncountered( "/anything" ) );
        assertFalse( empty.wasRead( "/anything" ) );
        assertFalse( empty.wasVetoed( "/anything" ) );
        assertFalse( empty.wasNotFound( "/anything" ) );
        assertFalse( empty.wasFault( "/anything" ) );
    }

    /**
     *  There is one definition of "is this a HOCON identifier", on ConfigUtils, used both to
     *  select a config source and to rewrite overlapping HOCON paths in
     *  ensureHoconInterresolvability. It moved here from BasicMultiPropertiesConfig, and
     *  loosened at the same time: it was once length-strictly-greater-than the prefix, and is
     *  now a plain prefix test, so a bare "hocon:" now counts.
     *
     *  That matches the treatment of the file: scheme, where two predicates that disagreed on
     *  the bare-scheme case were unified onto the permissive one. Degenerate identifiers are
     *  absorbed downstream as ordinary bad paths rather than being screened out here.
     */
    public void testIsHoconPathPredicate()
    {
        assertTrue( ConfigUtils.isHoconPath( "hocon:/reference" ) );
        assertTrue( "the prefix is case-insensitive", ConfigUtils.isHoconPath( "HOCON:/reference" ) );
        assertTrue( ConfigUtils.isHoconPath( "Hocon:/reference" ) );
        assertTrue( ConfigUtils.isHoconPath( "hocon:/a,/b,/" ) );
        assertTrue( "a bare prefix now counts, and degrades downstream",
                    ConfigUtils.isHoconPath( "hocon:" ) );

        assertFalse( ConfigUtils.isHoconPath( "/plain.properties" ) );
        assertFalse( ConfigUtils.isHoconPath( "/" ) );
        assertFalse( ConfigUtils.isHoconPath( "hoc" ) );
        assertFalse( "the prefix must lead", ConfigUtils.isHoconPath( "/hocon:/x" ) );
    }

    /** The other scheme predicate, for symmetry -- both are permissive prefix tests now. */
    public void testIsFileUrlIdentifierPredicate()
    {
        assertTrue( FileUrlPropertiesConfigSource.isFileUrlIdentifier( "file:/x" ) );
        assertTrue( FileUrlPropertiesConfigSource.isFileUrlIdentifier( "FILE:/x" ) );
        assertTrue( FileUrlPropertiesConfigSource.isFileUrlIdentifier( "file:" ) );

        assertFalse( FileUrlPropertiesConfigSource.isFileUrlIdentifier( "/x" ) );
        assertFalse( FileUrlPropertiesConfigSource.isFileUrlIdentifier( "hocon:/x" ) );
        assertFalse( FileUrlPropertiesConfigSource.isFileUrlIdentifier( "/file:/x" ) );
    }

    /**
     *  Source selection is what decides whether an identifier is vetoable, so the two must
     *  agree: an identifier routed to a VetoableConfig must also be reported as vetoable by
     *  the check MConfig.AsProvided uses to refuse it.
     */
    public void testVetoableDetectionAgreesWithSourceSelection()
    {
        java.util.List items = new java.util.ArrayList();

        assertTrue( "file: URLs are vetoable",
                    ConfigUtils.pointsToVetoableConfig( "file:/tmp/whatever.properties", items ) );
        assertTrue( ConfigUtils.pointsToVetoableConfig( "FILE:/tmp/whatever.properties", items ) );

        assertFalse( "classpath resources are not", ConfigUtils.pointsToVetoableConfig( "/some.properties", items ) );
        assertFalse( "System properties are not",   ConfigUtils.pointsToVetoableConfig( "/", items ) );

        // and the source actually selected for a file: URL really is a VetoableConfig
        PropertiesConfigSource pcs =
            ConfigUtils.propertiesConfigSourceForIdentifier( "file:/tmp/whatever.properties", items );
        assertTrue( "selection and detection must not diverge", pcs instanceof VetoableConfig );
    }

    /** Source instances are shared, so they must carry no per-read state. */
    public void testConfigSourceInstancesAreReused()
    {
        java.util.List items = new java.util.ArrayList();

        assertSame( ConfigUtils.propertiesConfigSourceForIdentifier( "/a.properties", items ),
                    ConfigUtils.propertiesConfigSourceForIdentifier( "/b.properties", items ) );
        assertSame( ConfigUtils.propertiesConfigSourceForIdentifier( "file:/a", items ),
                    ConfigUtils.propertiesConfigSourceForIdentifier( "file:/b", items ) );
        assertNotSame( "different schemes get different sources",
                       ConfigUtils.propertiesConfigSourceForIdentifier( "/a.properties", items ),
                       ConfigUtils.propertiesConfigSourceForIdentifier( "file:/a", items ) );
    }
}
