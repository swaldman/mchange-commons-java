package com.mchange.v2.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import com.mchange.v2.log.MLevel;
import com.mchange.v2.cfg.SealedSystemPropertiesStringProperty.Whitespace;

/**
 *  SealedSystemPropertiesStringProperty: System properties as of the seal, supplied
 *  configuration live, then a default.
 *
 *  <p>The String member of the sealed family, and the one whose precedence differs in kind. Its
 *  siblings resolve disagreement conservatively -- the safer answer wins, whichever source
 *  offered it -- because a boolean has a safe polarity and a whitelist a narrower reading. A
 *  String has neither, so a sealed System property simply outranks configuration.</p>
 *
 *  <p>Most of these cases are about whitespace, which is not fussiness: the intended values are
 *  class names bound for Class.forName, Properties.load preserves trailing whitespace, and a
 *  name with a stray space fails as a ClassNotFoundException for a class that looks perfectly
 *  correct in the configuration file.</p>
 */
public class SealedSystemPropertiesStringPropertyInternalJUnitTestCase extends TestCase
{
    private final static String PFX = "com.mchange.v2.cfg.junit.sssp";
    private final static String KEY = PFX + ".guardClass";

    private final static String FROM_SYS = "com.example.FromSysprops";
    private final static String FROM_CFG = "com.example.FromConfig";
    private final static String DEFAULT   = "com.example.Default";

    private CapturingMLogger logger;
    private Properties saved;

    @Override
    public void setUp() throws Exception
    {
        logger = new CapturingMLogger();
        saved = (Properties) System.getProperties().clone();
        clearOurKeys();
        SecurityRatchetTestSupport.unsealSystemProperties();
    }

    @Override
    public void tearDown() throws Exception
    {
        clearOurKeys();
        for ( String k : saved.stringPropertyNames() )
            if ( k.startsWith( PFX ) )
                System.setProperty( k, saved.getProperty( k ) );
        SecurityRatchetTestSupport.unsealSystemProperties();
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

    /** Begin again as an unstarted JVM would, so a property set now lands in the snapshot. */
    private static void asAFreshJvm() throws Exception
    { SecurityRatchetTestSupport.unsealSystemProperties(); }

    private static PropertiesConfig pcfg( String key, String value )
    {
        Properties p = new Properties();
        p.setProperty( key, value );
        return new BasicMultiPropertiesConfig( "/notional-test-resource", p );
    }

    private SealedSystemPropertiesStringProperty prop()
    { return new SealedSystemPropertiesStringProperty( KEY, DEFAULT ); }

    private String value( SealedSystemPropertiesStringProperty p )
    { return p.getValue( null, logger ); }

    private String value( SealedSystemPropertiesStringProperty p, PropertiesConfig cfg )
    { return p.getValue( cfg, logger ); }

    // ==================== precedence ====================

    public void testASealedSystemPropertyOutranksEverything()
    {
        System.setProperty( KEY, FROM_SYS );

        assertEquals( FROM_SYS, value( prop(), pcfg( KEY, FROM_CFG ) ) );
    }

    public void testConfigIsUsedWhenNoSystemPropertyWasSealed()
    { assertEquals( FROM_CFG, value( prop(), pcfg( KEY, FROM_CFG ) ) ); }

    public void testTheDefaultIsUsedWhenNothingIsConfigured()
    { assertEquals( DEFAULT, value( prop() ) ); }

    public void testNoDefaultMeansNull()
    { assertNull( value( new SealedSystemPropertiesStringProperty( KEY ) ) ); }

    /** A null PropertiesConfig is ordinary here -- ReferenceableUtils passes one on most paths. */
    public void testANullConfigIsNotAnError() throws Exception
    {
        assertEquals( DEFAULT, value( prop(), null ) );

        // that first lookup sealed; a property set now would be invisible, so start over
        asAFreshJvm();
        System.setProperty( KEY, FROM_SYS );
        assertEquals( FROM_SYS, value( prop(), null ) );
    }

    // ==================== the two sources, treated differently ====================

    public void testRuntimeSystemPropertyChangesAreInvisible()
    {
        System.setProperty( KEY, FROM_SYS );
        SealedSystemPropertiesStringProperty p = prop();
        assertEquals( FROM_SYS, value( p ) );

        System.setProperty( KEY, "com.example.Injected" );

        assertEquals( "A System property written after the seal must not be seen.",
                      FROM_SYS, value( p ) );
    }

    public void testASystemPropertySetAfterTheSealIsNeverSeen()
    {
        SealedSystemProperties.seal();
        System.setProperty( KEY, "com.example.Injected" );

        assertEquals( DEFAULT, value( prop() ) );
    }

    /** Configuration is a parameter: honored per call, and its influence ends with the call. */
    public void testConfigIsHonoredPerCallAndDoesNotPersist()
    {
        SealedSystemPropertiesStringProperty p = prop();

        assertEquals( FROM_CFG, value( p, pcfg( KEY, FROM_CFG ) ) );
        assertEquals( "Withdrawing configuration restores the default.", DEFAULT, value( p ) );
        assertEquals( "com.example.Other", value( p, pcfg( KEY, "com.example.Other" ) ) );
    }

    // ==================== whitespace ====================

    public void testValuesAreTrimmedFromEitherSource() throws Exception
    {
        System.setProperty( KEY, "  " + FROM_SYS + "  " );
        assertEquals( FROM_SYS, value( prop() ) );

        System.clearProperty( KEY );
        asAFreshJvm();
        assertEquals( FROM_CFG, value( prop(), pcfg( KEY, "\t" + FROM_CFG + "\t" ) ) );
    }

    /**
     *  Under the default policy a blank value is absent, so it falls through rather than
     *  yielding the empty String. A stray "someKey=" line must not become Class.forName("").
     */
    public void testABlankValueFallsThroughToTheNextSource()
    {
        assertEquals( DEFAULT, value( prop(), pcfg( KEY, "   " ) ) );
    }

    /**
     *  and because each source is trimmed as it is read, rather than once at the end, a blank
     *  System property yields to a real configured value instead of masking it.
     */
    public void testABlankSystemPropertyYieldsToRealConfiguration()
    {
        System.setProperty( KEY, "   " );

        assertEquals( FROM_CFG, value( prop(), pcfg( KEY, FROM_CFG ) ) );
    }

    public void testBlanksAreEmptyPolicyReturnsTheEmptyString()
    {
        SealedSystemPropertiesStringProperty p =
            new SealedSystemPropertiesStringProperty( KEY, DEFAULT, Whitespace.TRIM_BLANKS_ARE_EMPTY );

        assertEquals( "", value( p, pcfg( KEY, "   " ) ) );
    }

    public void testNoTrimPolicyPreservesWhatWasConfigured()
    {
        SealedSystemPropertiesStringProperty p =
            new SealedSystemPropertiesStringProperty( KEY, DEFAULT, Whitespace.NO_TRIM );

        assertEquals( "  spaced  ", value( p, pcfg( KEY, "  spaced  " ) ) );
    }

    // ==================== the default is used as-is ====================

    /**
     *  A default is a literal from the calling code, not configuration, so the whitespace
     *  policy does not touch it. Silently trimming would conceal an authoring mistake in the
     *  only place it could be fixed.
     */
    public void testTheDefaultIsNotProcessedByTheWhitespacePolicy()
    {
        SealedSystemPropertiesStringProperty p =
            new SealedSystemPropertiesStringProperty( KEY, "  " + DEFAULT + "  " );

        assertEquals( "  " + DEFAULT + "  ", value( p ) );
    }

    /** It is reported instead -- once, and whether or not the default is ever used. */
    public void testAMalformedDefaultIsReportedEvenWhenUnused()
    {
        System.setProperty( KEY, FROM_SYS );
        SealedSystemPropertiesStringProperty p =
            new SealedSystemPropertiesStringProperty( KEY, "  " + DEFAULT + "  " );

        assertEquals( "Precondition: the default is not in play.", FROM_SYS, value( p ) );
        assertEquals( "A defect in code should be reported however the deployment is configured: "
                      + logger.warnings(),
                      1, logger.warningsContaining( "used AS-IS" ).size() );

        logger.clear();
        value( p );
        assertTrue( "and not repeated: " + logger.warnings(),
                    logger.warningsContaining( "used AS-IS" ).isEmpty() );
    }

    public void testAWellFormedDefaultDrawsNoSuchComplaint()
    {
        value( prop() );

        assertTrue( "Nothing is wrong with this default: " + logger.warnings(),
                    logger.warningsContaining( "used AS-IS" ).isEmpty() );
    }

    /**
     *  An empty default is exempt under the policy that declares the empty String meaningful.
     *  Complaining there would tell an author they erred in doing exactly what they chose.
     */
    public void testAnEmptyDefaultIsNotComplainedAboutWhenBlanksAreEmpty()
    {
        SealedSystemPropertiesStringProperty p =
            new SealedSystemPropertiesStringProperty( KEY, "", Whitespace.TRIM_BLANKS_ARE_EMPTY );

        assertEquals( "", value( p ) );
        assertTrue( "The policy declared this value meaningful: " + logger.warnings(),
                    logger.warningsContaining( "used AS-IS" ).isEmpty() );
    }

    /** but an empty default is a contradiction under the policy where blanks mean absent. */
    public void testAnEmptyDefaultIsComplainedAboutWhenBlanksAreNull()
    {
        SealedSystemPropertiesStringProperty p =
            new SealedSystemPropertiesStringProperty( KEY, "", Whitespace.TRIM_BLANKS_ARE_NULL );

        value( p );

        assertEquals( 1, logger.warningsContaining( "used AS-IS" ).size() );
    }

    // ==================== notices about falling back ====================

    public void testFallingBackToTheDefaultIsReportedOnceThenReArms()
    {
        SealedSystemPropertiesStringProperty p = prop();

        assertEquals( DEFAULT, value( p ) );
        assertEquals( "Told once: " + logger.warnings(),
                      1, logger.warningsContaining( "Using default value" ).size() );

        logger.clear();
        assertEquals( DEFAULT, value( p ) );
        assertTrue( "not repeated while nothing changes: " + logger.warnings(),
                    logger.warnings().isEmpty() );

        assertEquals( FROM_CFG, value( p, pcfg( KEY, FROM_CFG ) ) );
        logger.clear();

        assertEquals( DEFAULT, value( p ) );
        assertEquals( "and told again once configuration is withdrawn: " + logger.warnings(),
                      1, logger.warningsContaining( "Using default value" ).size() );
    }

    /** Taking a sealed System property is normal, so it is noted informationally, and once. */
    public void testUsingASealedSystemPropertyIsNotedOnceAtInfo()
    {
        logger.setMinimumLevel( MLevel.INFO );
        System.setProperty( KEY, FROM_SYS );
        SealedSystemPropertiesStringProperty p = prop();

        value( p );
        assertEquals( "Told that other configuration will be ignored: " + logger.warnings(),
                      1, logger.warningsContaining( "will be ignored" ).size() );

        logger.clear();
        value( p );
        assertTrue( "but only once: " + logger.warnings(), logger.warnings().isEmpty() );
    }
}
