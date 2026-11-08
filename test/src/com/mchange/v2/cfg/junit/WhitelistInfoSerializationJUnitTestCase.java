package com.mchange.v2.cfg.junit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import com.mchange.v2.cfg.WhitelistInfo;
import com.mchange.v2.reflect.InstantiationNotPermittedException;
import com.mchange.v2.sql.SqlUtils;

/**
 *  The serialized forms of WhitelistInfo and InstantiationNotPermittedException.
 *
 *  <p>Both classes support serialization deliberately rather than incidentally, and for a reason
 *  that is invisible from either class alone: a refusal travels to application code as the
 *  <i>cause</i> of a SQLException, and a cause chain holding one non-serializable link cannot be
 *  written at all. A field that does not serialize therefore does not degrade such an exception,
 *  it replaces it with a NotSerializableException naming an implementation class, in whatever
 *  remote setting -- JMX, RMI, an application server -- happened to serialize it. That is the
 *  failure {@link #testARefusalSurvivesAsTheCauseOfASQLException} exists for, and it is the case
 *  nobody will think to re-check when a field is added to either class.</p>
 *
 *  <p>The rest turn on the fact that deserialization bypasses constructors. WhitelistInfo's
 *  constructor refuses null components and hardens its sets against modification; readObject must
 *  do both, or a stream can produce an instance in a state the class says cannot exist -- and
 *  since the thing everything logs is its toString(), such an instance means an NPE thrown from
 *  inside a log statement while reporting a security refusal.</p>
 *
 *  <p>The malformed cases reach WhitelistInfo's private fields reflectively. That is not a
 *  shortcut around the API but the whole point: what they simulate is a stream that never ran a
 *  constructor, and no amount of public API can produce an instance the constructor would have
 *  refused. Writing such a stream by hand instead would pin this library's serialized layout into
 *  a literal byte array, which is worse -- the format is versioned precisely so it can change.</p>
 *
 *  <p>Note what none of this makes true: validation leaves a deserialized instance well-formed,
 *  not trustworthy. A forged stream may still describe a whitelist that was never configured.
 *  That is tolerable only because nothing consults these objects to decide anything -- the gate
 *  resolves the whitelist afresh from live configuration every time -- so there is deliberately
 *  no test here asserting that a deserialized whitelist can be relied upon. It cannot.</p>
 */
public class WhitelistInfoSerializationJUnitTestCase extends TestCase
{
    private final static String REFUSED = "com.example.NotWhitelisted";

    private static Set<String> mutableSet( String... elements )
    { return new HashSet<String>( Arrays.asList( elements ) ); }

    private static WhitelistInfo info()
    {
        return new WhitelistInfo( mutableSet( "a.B", "c.D" ),
                                  mutableSet( "some.key", "other.key" ),
                                  WhitelistInfo.Source.MAIN_WHITELIST );
    }

    private static Object roundTrip( Object o ) throws IOException, ClassNotFoundException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        try { oos.writeObject( o ); }
        finally { oos.close(); }

        ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );
        try { return ois.readObject(); }
        finally { ois.close(); }
    }

    /**
     *  A WhitelistInfo whose named field has been set to null after construction, which the
     *  constructor would have refused. Serializing this is how a hostile or corrupt stream is
     *  simulated without hand-assembling bytes.
     */
    private static WhitelistInfo withFieldSet( String fieldName, Object value ) throws Exception
    {
        WhitelistInfo out = info();
        Field f = WhitelistInfo.class.getDeclaredField( fieldName );
        f.setAccessible( true );
        f.set( out, value );
        return out;
    }

    /**
     *  As {@link #withFieldSet}, for InstantiationNotPermittedException's own fields. Reflection
     *  again, and for the same reason: the constructor refuses these states, so a stream carrying
     *  one cannot be produced any other way.
     */
    private static InstantiationNotPermittedException exceptionWithFieldSet( String fieldName, Object value )
        throws Exception
    {
        InstantiationNotPermittedException out =
            new InstantiationNotPermittedException( "refused", REFUSED, info() );
        Field f = InstantiationNotPermittedException.class.getDeclaredField( fieldName );
        f.setAccessible( true );
        f.set( out, value );
        return out;
    }

    private static void assertExceptionStreamRefuses( String fieldName, Object value ) throws Exception
    {
        try
        {
            roundTrip( exceptionWithFieldSet( fieldName, value ) );
            fail( "a stream supplying a null '" + fieldName + "' must be refused, because the "
                      + "constructor refuses it and deserialization must not be a way around that." );
        }
        catch ( InvalidObjectException expected )
        {}
    }

    private static void assertStreamRefuses( String fieldName ) throws Exception
    {
        try
        {
            roundTrip( withFieldSet( fieldName, null ) );
            fail( "A stream supplying a null '" + fieldName + "' must be refused, because the "
                      + "constructor refuses it and deserialization must not be a way around that." );
        }
        catch ( InvalidObjectException expected )
        {
            // InvalidObjectException specifically, and not the IllegalArgumentException the
            // constructor throws: it is an IOException, which is what code around a
            // readObject() call is written to catch. An unchecked exception escaping
            // deserialization slips past those handlers.
            assertTrue( "the refusal should say which component was missing, so an operator "
                            + "reading a remote stack trace can tell: " + expected.getMessage(),
                        expected.getMessage() != null && expected.getMessage().length() > 0 );
        }
    }

    public void testWhitelistInfoRoundTripsIntact() throws Exception
    {
        WhitelistInfo original = info();
        WhitelistInfo revived  = (WhitelistInfo) roundTrip( original );

        assertEquals( "a WhitelistInfo must survive serialization whole; equality covers the "
                          + "whitelist, the keys it was computed from, and the governing source",
                      original, revived );
        // Its rendering is embedded verbatim in messages elsewhere, so it must still carry every
        // part. Note it is NOT compared string-to-string: toString() renders Sets, whose
        // iteration order no stream preserves, so an equality assertion there would pass or fail
        // on hash order rather than on anything this class promises.
        String rendered = revived.toString();
        assertTrue( "the rendering must still name the governing source: " + rendered,
                    rendered.indexOf( WhitelistInfo.Source.MAIN_WHITELIST.getIdentifier() ) >= 0 );
        for ( String entry : original.getWhitelist() )
            assertTrue( "the rendering must still name whitelist entry '" + entry + "': " + rendered,
                        rendered.indexOf( entry ) >= 0 );
        for ( String key : original.getFromKeys() )
            assertTrue( "the rendering must still name contributing key '" + key + "': " + rendered,
                        rendered.indexOf( key ) >= 0 );
    }

    /**
     *  The case the whole serialized form exists for. c3p0 delivers a refused driverClass to
     *  application code as a SQLException with the refusal as its cause; SQLException is
     *  serializable, and serializing a chain serializes every link.
     */
    public void testARefusalSurvivesAsTheCauseOfASQLException() throws Exception
    {
        InstantiationNotPermittedException inpe =
            new InstantiationNotPermittedException( "refused by whitelist", REFUSED, info() );
        SQLException sqle = SqlUtils.toSQLException( "could not acquire a Connection", inpe );

        SQLException revived = (SQLException) roundTrip( sqle );
        Throwable cause = revived.getCause();

        assertTrue( "the refusal must still be the cause, not lost to a NotSerializableException "
                        + "raised while writing the chain; got: " + cause,
                    cause instanceof InstantiationNotPermittedException );

        InstantiationNotPermittedException revivedInpe = (InstantiationNotPermittedException) cause;
        assertEquals( "and must still name what was refused, which is the actionable part",
                      REFUSED, revivedInpe.getRefused() );
        assertEquals( "and what it was refused against", info(), revivedInpe.getWhitelistInfo() );
    }

    public void testTheExceptionRoundTripsIntact() throws Exception
    {
        InstantiationNotPermittedException original =
            new InstantiationNotPermittedException( "refused by whitelist", REFUSED, info(),
                                                    new IllegalStateException( "underlying" ) );
        InstantiationNotPermittedException revived =
            (InstantiationNotPermittedException) roundTrip( original );

        assertEquals( "message", original.getMessage(), revived.getMessage() );
        assertEquals( "refused name", REFUSED, revived.getRefused() );
        assertEquals( "whitelist", original.getWhitelist(), revived.getWhitelist() );
        assertEquals( "whitelist info", original.getWhitelistInfo(), revived.getWhitelistInfo() );
        assertEquals( "a cause, where there was one, is Throwable's own business but must not be "
                          + "dropped by our taking over the rest of the form",
                      IllegalStateException.class, revived.getCause().getClass() );
    }

    /**
     *  Both components are required, so that a handler may read them without guarding. c3p0's
     *  DriverManagerDataSource composes its SQLException message straight from
     *  getWhitelistInfo(), and a null there would mean an NPE raised inside the catch handler of
     *  a security gate -- the refusal replaced by an unrelated failure, which is the one outcome
     *  worse than a long message.
     */
    public void testTheConstructorRefusesANullRefusedName()
    {
        try
        {
            new InstantiationNotPermittedException( "refused", null, info() );
            fail( "a refusal that cannot say what it refused must not be constructible" );
        }
        catch ( IllegalArgumentException expected )
        {}
    }

    public void testTheConstructorRefusesANullWhitelistInfo()
    {
        try
        {
            new InstantiationNotPermittedException( "refused", REFUSED, null );
            fail( "a refusal that cannot say what it was refused against must not be constructible" );
        }
        catch ( IllegalArgumentException expected )
        {}
    }

    /**
     *  And the same invariants across a stream, which is the half the constructor cannot defend.
     *  Written with writeObject rather than writeUTF, the form can represent a null that the
     *  constructor would have rejected, so readObject has to reject it too -- otherwise
     *  deserialization is a way to manufacture exactly the instance the checks above forbid.
     */
    public void testAStreamWithANullRefusedNameIsRefused() throws Exception
    { assertExceptionStreamRefuses( "refused", null ); }

    public void testAStreamWithANullWhitelistInfoIsRefused() throws Exception
    { assertExceptionStreamRefuses( "whitelistInfo", null ); }

    public void testAStreamWithANullWhitelistIsRefused() throws Exception
    { assertStreamRefuses( "whitelist" ); }

    public void testAStreamWithANullKeySetIsRefused() throws Exception
    { assertStreamRefuses( "fromKeys" ); }

    /**
     *  The null that bites hardest: toString() switches on the source, so an instance carrying
     *  none throws NPE from inside whatever log statement reports the refusal.
     */
    public void testAStreamWithANullSourceIsRefused() throws Exception
    { assertStreamRefuses( "source" ); }

    /**
     *  A stream carries whatever Set implementation was written into it, so the unmodifiable
     *  wrapper the constructor applies cannot be assumed to be there. readObject must re-apply
     *  it, or an accessor documented as unmodifiable returns something a caller can widen.
     */
    public void testAStreamSupplyingAMutableSetIsReHardened() throws Exception
    {
        // bare HashSets, as a crafted stream would carry in place of the unmodifiable wrappers
        WhitelistInfo hostile = withFieldSet( "whitelist", mutableSet( "a.B", "c.D" ) );
        Field fromKeys = WhitelistInfo.class.getDeclaredField( "fromKeys" );
        fromKeys.setAccessible( true );
        fromKeys.set( hostile, mutableSet( "some.key" ) );

        WhitelistInfo revived = (WhitelistInfo) roundTrip( hostile );

        try
        {
            revived.getWhitelist().add( "com.example.Sneaky" );
            fail( "a deserialized whitelist must be unmodifiable, exactly as a constructed one is" );
        }
        catch ( UnsupportedOperationException expected )
        {}

        try
        {
            revived.getFromKeys().add( "sneaky.key" );
            fail( "and so must the set of keys it was computed from" );
        }
        catch ( UnsupportedOperationException expected )
        {}
    }

    /**
     *  An empty whitelist is a legitimate resolution -- it means deny everything -- so it has to
     *  survive as an empty set rather than being normalized into anything else. There is no
     *  "missing whitelist" case to distinguish it from any more: a refusal must now carry a
     *  WhitelistInfo, and Source.MISSING is how an unconfigured whitelist says so while still
     *  reporting the empty set it resolved to.
     */
    public void testAnEmptyWhitelistRoundTripsAsEmptyRatherThanAsNothing() throws Exception
    {
        WhitelistInfo empty = new WhitelistInfo( Collections.<String>emptySet(),
                                                 mutableSet( "some.key" ),
                                                 WhitelistInfo.Source.MAIN_WHITELIST );
        InstantiationNotPermittedException revived = (InstantiationNotPermittedException)
            roundTrip( new InstantiationNotPermittedException( "refused", REFUSED, empty ) );

        assertNotNull( "a deny-all whitelist must arrive as a set, not as null", revived.getWhitelist() );
        assertTrue( "and that set must be empty, which is what denies everything",
                    revived.getWhitelist().isEmpty() );
        assertEquals( "and the resolution it came from must be intact", empty, revived.getWhitelistInfo() );
    }
}
