package com.mchange.v2.cfg;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 *  SealedSystemProperties: a one-time snapshot of System properties, so that configuration
 *  whose meaning must not change after startup cannot be rewritten afterward.
 *
 *  <p>Sealing is a one-shot static, once per JVM, and mill runs the whole suite in a single
 *  JVM (testParallelism = false). These tests therefore reset the seal reflectively around
 *  each case. That is deliberate and confined to the test: the production class offers no
 *  reset, and should not -- a seal that could be undone would not be a seal. Resetting here
 *  is also what keeps these cases from poisoning every later test class, since the first
 *  thing to consult configuration would otherwise fix the snapshot for the whole run.</p>
 */
public class SealedSystemPropertiesInternalJUnitTestCase extends TestCase
{
    private final static String PFX = "com.mchange.v2.cfg.junit.sealed";
    private final static String KEY = PFX + ".key";

    private Properties saved;

    @Override
    public void setUp() throws Exception
    {
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
        unseal();
    }

    @Override
    public void tearDown() throws Exception
    {
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                System.setProperty( k, saved.getProperty( k ) );
        unseal();
    }

    /** Restores the pre-sealed state; see the class comment for why this exists only here. */
    private static void unseal() throws Exception
    {
        Field f = SealedSystemProperties.class.getDeclaredField( "theProperties" );
        f.setAccessible( true );
        f.set( null, null );
    }

    private void clearOurKeys()
    {
        List<String> doomed = new ArrayList<String>();
        for ( String k : System.getProperties().stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                doomed.add( k );
        for ( String k : doomed )
            System.clearProperty( k );
    }

    // ---------- when the seal happens ----------

    /** Asking must not answer by changing the answer. */
    public void testIsSealedDoesNotItselfSeal()
    {
        assertFalse( SealedSystemProperties.isSealed() );
        assertFalse( "Repeated interrogation must still not seal.", SealedSystemProperties.isSealed() );
    }

    /**
     *  get() is not a passive accessor: the first call fixes the snapshot. This is the
     *  lazy half of the design, and the reason any consumer can be the one that seals.
     */
    public void testGetSeals()
    {
        assertFalse( SealedSystemProperties.isSealed() );
        SealedSystemProperties.get();
        assertTrue( SealedSystemProperties.isSealed() );
    }

    /** seal() reports whether the call is what sealed, so a caller can tell it was too late. */
    public void testSealReportsWhetherItWasTheSealer()
    {
        assertTrue( "The first call seals and says so.", SealedSystemProperties.seal() );
        assertTrue( SealedSystemProperties.isSealed() );
        assertFalse( "A later call cannot re-seal, and says so.", SealedSystemProperties.seal() );
    }

    public void testSealAfterAnIncidentalGetReportsFalse()
    {
        SealedSystemProperties.get();

        assertFalse( "Something else already sealed; seal() must not claim otherwise.",
                     SealedSystemProperties.seal() );
    }

    // ---------- what the snapshot contains ----------

    public void testPropertiesSetBeforeSealingAreVisible()
    {
        System.setProperty( KEY, "before" );
        SealedSystemProperties.seal();

        assertEquals( "before", SealedSystemProperties.get().getProperty( KEY ) );
    }

    /** The point of the whole class: a later write cannot change what was sealed. */
    public void testPropertiesSetAfterSealingAreInvisible()
    {
        System.setProperty( KEY, "before" );
        SealedSystemProperties.seal();

        System.setProperty( KEY, "after" );

        assertEquals( "The snapshot must not follow the live value.",
                      "before", SealedSystemProperties.get().getProperty( KEY ) );
        assertEquals( "Precondition: the live property really did change.",
                      "after", System.getProperty( KEY ) );
    }

    /** and neither can a later removal. */
    public void testPropertiesClearedAfterSealingRemainVisible()
    {
        System.setProperty( KEY, "before" );
        SealedSystemProperties.seal();

        System.clearProperty( KEY );

        assertEquals( "before", SealedSystemProperties.get().getProperty( KEY ) );
        assertNull( System.getProperty( KEY ) );
    }

    /** A key created only after the seal is not in it at all. */
    public void testPropertiesCreatedAfterSealingAreAbsent()
    {
        SealedSystemProperties.seal();
        System.setProperty( KEY, "late" );

        assertNull( SealedSystemProperties.get().getProperty( KEY ) );
    }

    public void testGetReturnsTheSameSnapshotEveryTime()
    {
        PropertiesConfig first = SealedSystemProperties.get();

        System.setProperty( KEY, "churn" );

        assertSame( "There is exactly one snapshot per JVM.", first, SealedSystemProperties.get() );
    }

    // ---------- the MConfig facade ----------

    /** MConfig is where callers look first; its forwarders must mean the same thing. */
    public void testMConfigForwardersAgree()
    {
        assertFalse( MConfig.isSealedSystemProperties() );

        System.setProperty( KEY, "via-mconfig" );
        assertTrue( "MConfig.sealSystemProperties() must seal.", MConfig.sealSystemProperties() );

        assertTrue( MConfig.isSealedSystemProperties() );
        assertSame( SealedSystemProperties.get(), MConfig.getSealedSystemProperties() );
        assertEquals( "via-mconfig", MConfig.getSealedSystemProperties().getProperty( KEY ) );
        assertFalse( MConfig.sealSystemProperties() );
    }
}
