package com.mchange.v2.naming.junit;

import java.util.Properties;

import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.PropertiesConfig;
import com.mchange.v2.cfg.SecurityRatchetTestSupport;
import com.mchange.v2.naming.SecurelyStringifiable;
import com.mchange.v2.naming.SecurelyStringifiableException;
import com.mchange.v2.reflect.ByNameInstantiationUtils;
import com.mchange.v2.reflect.InstantiationNotPermittedException;
import junit.framework.TestCase;

public final class SecurelyStringifiableJUnitTestCase extends TestCase
{
    // ==========================================
    // Stub classes for testing
    // ==========================================

    /** Fully conforming: has both methods with correct signatures. */
    public static final class Point
    {
        public final int x;
        public final int y;

        public Point( int x, int y )
        {
            this.x = x;
            this.y = y;
        }

        public static String securelyStringify( Point p )
        { return p.x + "," + p.y; }

        public static Point constructSecurelyStringified( String s, PropertiesConfig pcfg )
        {
            String[] parts = s.split( "," );
            return new Point( Integer.parseInt( parts[0] ), Integer.parseInt( parts[1] ) );
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o ) return true;
            if ( !( o instanceof Point ) ) return false;
            Point other = (Point) o;
            return this.x == other.x && this.y == other.y;
        }

        @Override
        public int hashCode()
        { return 31 * x + y; }

        @Override
        public String toString()
        { return "Point(" + x + "," + y + ")"; }
    }

    /** Has securelyStringify but not constructSecurelyStringified. */
    public static final class StringifyOnly
    {
        public final String value;
        public StringifyOnly( String v ) { this.value = v; }

        public static String securelyStringify( StringifyOnly o )
        { return o.value; }
    }

    /** Has constructSecurelyStringified but not securelyStringify. */
    public static final class ConstructOnly
    {
        public final String value;
        public ConstructOnly( String v ) { this.value = v; }

        public static ConstructOnly constructSecurelyStringified( String s, PropertiesConfig pcfg )
        { return new ConstructOnly( s ); }
    }

    /** Has neither method. */
    public static final class NoMethods
    {
        public final String value;
        public NoMethods( String v ) { this.value = v; }
    }

    /**
     *  Conforms to the contract as it stood before 0.7.0, taking the stringified form alone.
     *  No longer recognized -- the PropertiesConfig is what carries the whitelist to a nested
     *  reconstruction, so a class that cannot accept one cannot be gated at depth.
     */
    public static final class OldSingleArgumentContract
    {
        public static String securelyStringify( OldSingleArgumentContract o ) { return "x"; }
        public static OldSingleArgumentContract constructSecurelyStringified( String s )
        { return new OldSingleArgumentContract(); }
    }

    /** Written to by Gadget's static initializer. Separate class, so reading it initializes nothing. */
    public static final class InitializerWitness
    {
        public static boolean gadgetInitializerRan = false;
    }

    /**
     *  Stands in for any class on the classpath whose static initializer does something. It is
     *  not SecurelyStringifiable, and nothing should ever cause it to be initialized.
     */
    public static final class Gadget
    {
        static { InitializerWitness.gadgetInitializerRan = true; }
    }

    /** Whitelisted in these tests, and reconstructs a nested value that may not be. */
    public static final class Outer
    {
        public static String securelyStringify( Outer o ) { return "ignored"; }
        public static Outer constructSecurelyStringified( String s, PropertiesConfig pcfg ) throws Exception
        {
            SecurelyStringifiable.constructSecurelyStringified( stringifiedOf( Inner.class, "inner-payload" ), pcfg );
            return new Outer();
        }
    }

    public static final class Inner
    {
        public static String securelyStringify( Inner i ) { return "ignored"; }
        public static Inner constructSecurelyStringified( String s, PropertiesConfig pcfg ) { return new Inner(); }
    }

    // ==========================================
    // Helpers
    // ==========================================

    private static String stringifiedOf( Class<?> cl, String payload )
    { return "Securely Stringified: " + cl.getName() + "\n" + payload; }

    /** Enforcement and a whitelist, supplied the way an application supplies configuration. */
    private static PropertiesConfig enforcing( Class<?>... whitelisted )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < whitelisted.length; ++i )
        {
            if ( i > 0 ) sb.append( ',' );
            sb.append( whitelisted[i].getName() );
        }
        Properties p = new Properties();
        p.setProperty( "com.mchange.v2.reflect.byNameInstantiation.enforceWhitelist", "true" );
        p.setProperty( "com.mchange.v2.reflect.byNameInstantiation.whitelist", sb.toString() );
        return MultiPropertiesConfig.fromProperties( "/notional-test-resource", p );
    }

    @Override
    protected void setUp() throws Exception
    { SecurityRatchetTestSupport.resetAll( ByNameInstantiationUtils.class ); }

    @Override
    protected void tearDown() throws Exception
    { SecurityRatchetTestSupport.resetAll( ByNameInstantiationUtils.class ); }

    // ==========================================
    // isSecurelyStringifiable
    // ==========================================

    public void testIsSecurelyStringifiableConformingClass()
    { assertTrue( SecurelyStringifiable.isSecurelyStringifiable( Point.class ) ); }

    public void testIsSecurelyStringifiableStringifyOnly()
    { assertFalse( SecurelyStringifiable.isSecurelyStringifiable( StringifyOnly.class ) ); }

    public void testIsSecurelyStringifiableConstructOnly()
    { assertFalse( SecurelyStringifiable.isSecurelyStringifiable( ConstructOnly.class ) ); }

    public void testIsSecurelyStringifiableNoMethods()
    { assertFalse( SecurelyStringifiable.isSecurelyStringifiable( NoMethods.class ) ); }

    public void testIsSecurelyStringifiableArbitraryClass()
    { assertFalse( SecurelyStringifiable.isSecurelyStringifiable( String.class ) ); }

    // ==========================================
    // securelyStringify
    // ==========================================

    public void testSecurelyStringifyConformingObject() throws Exception
    {
        Point p = new Point( 3, 7 );
        String result = SecurelyStringifiable.securelyStringify( p );
        String expectedFqcn = Point.class.getName();
        assertEquals( "Securely Stringified: " + expectedFqcn + "\n" + "3,7", result );
    }

    public void testSecurelyStringifyStringifyOnlyThrows()
    {
        try
        {
            SecurelyStringifiable.securelyStringify( new StringifyOnly( "hello" ) );
            fail( "Expected SecurelyStringifiableException: missing constructSecurelyStringified" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    public void testSecurelyStringifyConstructOnlyThrows()
    {
        try
        {
            SecurelyStringifiable.securelyStringify( new ConstructOnly( "hello" ) );
            fail( "Expected SecurelyStringifiableException: missing securelyStringify" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    public void testSecurelyStringifyNoMethodsThrows() throws Exception
    {
        try
        {
            SecurelyStringifiable.securelyStringify( new NoMethods( "hello" ) );
            fail( "Expected SecurelyStringifiableException: no methods at all" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    // ==========================================
    // constructSecurelyStringified
    // ==========================================

    public void testConstructSecurelyStringifiedConformingClass() throws Exception
    {
        String stringified = "Securely Stringified: " + Point.class.getName() + "\n" + "5,11";
        Object result = SecurelyStringifiable.constructSecurelyStringified( stringified, null );
        assertTrue( result instanceof Point );
        Point p = (Point) result;
        assertEquals( 5, p.x );
        assertEquals( 11, p.y );
    }

    public void testConstructSecurelyStringifiedStringifyOnlyThrows() throws Exception
    {
        try
        {
            String stringified = "Securely Stringified: " + StringifyOnly.class.getName() + "\n" + "hello";
            SecurelyStringifiable.constructSecurelyStringified( stringified, null );
            fail( "Expected SecurelyStringifiableException: missing constructSecurelyStringified" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    public void testConstructSecurelyStringifiedConstructOnlyThrows() throws Exception
    {
        try
        {
            String stringified = "Securely Stringified: " + ConstructOnly.class.getName() + "\n" + "hello";
            SecurelyStringifiable.constructSecurelyStringified( stringified, null );
            fail( "Expected SecurelyStringifiableException: missing securelyStringify" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    public void testConstructSecurelyStringifiedNoMethodsThrows() throws Exception
    {
        try
        {
            String stringified = "Securely Stringified: " + NoMethods.class.getName() + "\n" + "hello";
            SecurelyStringifiable.constructSecurelyStringified( stringified, null );
            fail( "Expected SecurelyStringifiableException: no methods at all" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    public void testConstructSecurelyStringifiedBadPrefixThrows() throws Exception
    {
        try
        {
            SecurelyStringifiable.constructSecurelyStringified( "not a valid stringified", null );
            fail( "Expected SecurelyStringifiableException: bad prefix" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    public void testConstructSecurelyStringifiedUnknownClassThrows() throws Exception
    {
        try
        {
            SecurelyStringifiable.constructSecurelyStringified( "Securely Stringified: com.nonexistent.FakeClass\nhello", null );
            fail( "Expected SecurelyStringifiableException: unknown class" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    // ==========================================
    // Round-trip: stringify then construct
    // ==========================================

    public void testRoundTrip() throws Exception
    {
        Point original = new Point( -42, 100 );
        String stringified = SecurelyStringifiable.securelyStringify( original );
        Object reconstructed = SecurelyStringifiable.constructSecurelyStringified( stringified, null );
        assertEquals( original, reconstructed );
    }

    public void testRoundTripOrigin() throws Exception
    {
        Point original = new Point( 0, 0 );
        String stringified = SecurelyStringifiable.securelyStringify( original );
        Object reconstructed = SecurelyStringifiable.constructSecurelyStringified( stringified, null );
        assertEquals( original, reconstructed );
    }

    // ==========================================
    // Gating a reconstruction that came off an untrusted Reference
    //
    // A stringified form names the class to reconstruct, and that name reaches us from whoever
    // wrote the Reference. Two things stand between it and arbitrary code: the by-name
    // instantiation whitelist, and the refusal to initialize a class before it has been shown to
    // be something we can reconstruct at all.
    // ==========================================

    /**
     *  Class.forName(String) initializes. So resolving an attacker-named class and *then*
     *  checking whether it is SecurelyStringifiable runs the class's static initializer before
     *  anything has validated it -- the shape check refuses it afterward, too late to matter.
     *  Loading without initializing makes the refusal mean something.
     *
     *  <p>This holds with nothing configured, which is the point: the whitelist defaults to
     *  warn-only, so in an ordinary deployment this is the whole of the protection.</p>
     */
    public void testAnUnsuitableClassIsRefusedWithoutEverBeingInitialized() throws Exception
    {
        assertFalse( "Precondition: nothing has initialized Gadget yet.",
                     InitializerWitness.gadgetInitializerRan );
        try
        {
            SecurelyStringifiable.constructSecurelyStringified( stringifiedOf( Gadget.class, "payload" ), null );
            fail( "Expected refusal: Gadget is not SecurelyStringifiable" );
        }
        catch (Exception e) { /* expected */ }

        assertFalse( "A class we refused must not have run its static initializer on the way to being refused.",
                     InitializerWitness.gadgetInitializerRan );
    }

    /** Under enforcement, the refusal comes earlier still, and says what was refused and why. */
    public void testAnUnwhitelistedClassIsRefusedByTheWhitelistWhenEnforced() throws Exception
    {
        try
        {
            SecurelyStringifiable.constructSecurelyStringified( stringifiedOf( Gadget.class, "payload" ),
                                                               enforcing( Point.class ) );
            fail( "Expected InstantiationNotPermittedException" );
        }
        catch (InstantiationNotPermittedException e)
        {
            assertTrue( "Should name what was refused: " + e.getMessage(),
                        e.getMessage().contains( Gadget.class.getName() ) );
        }
        assertFalse( InitializerWitness.gadgetInitializerRan );
    }

    /** A whitelisted, conforming class still reconstructs while enforcement is on. */
    public void testAWhitelistedClassIsStillReconstructedUnderEnforcement() throws Exception
    {
        Object out = SecurelyStringifiable.constructSecurelyStringified(
            stringifiedOf( Point.class, "5,11" ), enforcing( Point.class ) );

        assertEquals( new Point( 5, 11 ), out );
    }

    // ==========================================
    // The whitelist has to mean the same thing at every depth
    //
    // A stringified property may itself contain stringified properties, so a reconstruction can
    // recurse. Requiring the PropertiesConfig in the contract is what lets an application-supplied
    // whitelist reach those inner reconstructions; without it they would be gated against sealed
    // System properties only, and a whitelist set in application configuration would be invisible
    // one level down.
    // ==========================================

    public void testAConfiguredWhitelistReachesANestedReconstruction() throws Exception
    {
        Object out = SecurelyStringifiable.constructSecurelyStringified(
            stringifiedOf( Outer.class, "outer-payload" ), enforcing( Outer.class, Inner.class ) );

        assertTrue( "Both were whitelisted, in configuration alone.", out instanceof Outer );
    }

    public void testANestedClassLeftOffTheWhitelistIsStillRefused() throws Exception
    {
        try
        {
            SecurelyStringifiable.constructSecurelyStringified(
                stringifiedOf( Outer.class, "outer-payload" ), enforcing( Outer.class ) );
            fail( "Expected InstantiationNotPermittedException for the nested class" );
        }
        catch (InstantiationNotPermittedException e)
        {
            assertTrue( "Whitelisting the outer class must not whitelist what it reconstructs: " + e.getMessage(),
                        e.getMessage().contains( Inner.class.getName() ) );
        }
        catch (Exception e)
        { fail( "A refusal one level down is still a refusal, not a decoding failure: " + e ); }
    }

    // ==========================================
    // The pre-0.7.0 contract
    // ==========================================

    /** Deliberately incompatible: a class that cannot take a PropertiesConfig cannot be gated. */
    public void testTheOldSingleArgumentContractIsNoLongerRecognized() throws Exception
    {
        assertFalse( SecurelyStringifiable.isSecurelyStringifiable( OldSingleArgumentContract.class ) );
    }

    public void testRoundTripNegativeCoordinates() throws Exception
    {
        Point original = new Point( -1, -2 );
        String stringified = SecurelyStringifiable.securelyStringify( original );
        Object reconstructed = SecurelyStringifiable.constructSecurelyStringified( stringified, null );
        assertEquals( original, reconstructed );
    }
}
