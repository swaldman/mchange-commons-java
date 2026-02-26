package com.mchange.v2.naming.junit;

import com.mchange.v2.naming.SecurelyStringifiable;
import com.mchange.v2.naming.SecurelyStringifiableException;
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

        public static Point constructSecurelyStringified( String s )
        {
            String[] parts = s.split( "," );
            return new Point( Integer.parseInt( parts[0] ), Integer.parseInt( parts[1] ) );
        }

        public boolean equals( Object o )
        {
            if ( this == o ) return true;
            if ( !( o instanceof Point ) ) return false;
            Point other = (Point) o;
            return this.x == other.x && this.y == other.y;
        }

        public int hashCode()
        { return 31 * x + y; }

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

        public static ConstructOnly constructSecurelyStringified( String s )
        { return new ConstructOnly( s ); }
    }

    /** Has neither method. */
    public static final class NoMethods
    {
        public final String value;
        public NoMethods( String v ) { this.value = v; }
    }

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

    public void testSecurelyStringifyConformingObject() throws SecurelyStringifiableException
    {
        Point p = new Point( 3, 7 );
        String result = SecurelyStringifiable.securelyStringify( p );
        assertEquals( "3,7", result );
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

    public void testSecurelyStringifyNoMethodsThrows()
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

    public void testConstructSecurelyStringifiedConformingClass() throws SecurelyStringifiableException
    {
        Object result = SecurelyStringifiable.constructSecurelyStringified( Point.class, "5,11" );
        assertTrue( result instanceof Point );
        Point p = (Point) result;
        assertEquals( 5, p.x );
        assertEquals( 11, p.y );
    }

    public void testConstructSecurelyStringifiedStringifyOnlyThrows()
    {
        try
        {
            SecurelyStringifiable.constructSecurelyStringified( StringifyOnly.class, "hello" );
            fail( "Expected SecurelyStringifiableException: missing constructSecurelyStringified" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    public void testConstructSecurelyStringifiedConstructOnlyThrows()
    {
        try
        {
            SecurelyStringifiable.constructSecurelyStringified( ConstructOnly.class, "hello" );
            fail( "Expected SecurelyStringifiableException: missing securelyStringify" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    public void testConstructSecurelyStringifiedNoMethodsThrows()
    {
        try
        {
            SecurelyStringifiable.constructSecurelyStringified( NoMethods.class, "hello" );
            fail( "Expected SecurelyStringifiableException: no methods at all" );
        }
        catch ( SecurelyStringifiableException e ) { /* expected */ }
    }

    // ==========================================
    // Round-trip: stringify then construct
    // ==========================================

    public void testRoundTrip() throws SecurelyStringifiableException
    {
        Point original = new Point( -42, 100 );
        String stringified = SecurelyStringifiable.securelyStringify( original );
        Object reconstructed = SecurelyStringifiable.constructSecurelyStringified( Point.class, stringified );
        assertEquals( original, reconstructed );
    }

    public void testRoundTripOrigin() throws SecurelyStringifiableException
    {
        Point original = new Point( 0, 0 );
        String stringified = SecurelyStringifiable.securelyStringify( original );
        Object reconstructed = SecurelyStringifiable.constructSecurelyStringified( Point.class, stringified );
        assertEquals( original, reconstructed );
    }

    public void testRoundTripNegativeCoordinates() throws SecurelyStringifiableException
    {
        Point original = new Point( -1, -2 );
        String stringified = SecurelyStringifiable.securelyStringify( original );
        Object reconstructed = SecurelyStringifiable.constructSecurelyStringified( Point.class, stringified );
        assertEquals( original, reconstructed );
    }
}
