package com.mchange.v1.lang.junit;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import com.mchange.v1.lang.Synchronizer;

/**
 *  Exercises Synchronizer.createSynchronizedWrapper: that the proxy delegates and
 *  actually synchronizes, that exceptions reach the caller unmangled, and that the
 *  documented restriction to public interfaces fails fast rather than deferring to
 *  a confusing failure at the first call.
 *
 *  The exception cases matter more than they look. The handler delegates through
 *  Method.invoke, which wraps EVERYTHING a target throws -- checked, unchecked and
 *  Error alike -- in InvocationTargetException. If the handler does not unwrap that,
 *  the proxy sees a checked exception the interface does not declare and buries it in
 *  UndeclaredThrowableException. That defect made even RuntimeExceptions arrive
 *  double-wrapped, so no amount of exception-hierarchy design downstream could get a
 *  meaningful type through a synchronized wrapper.
 */
public class SynchronizerJUnitTestCase extends TestCase
{
    // ------------------------------------------------------------- fixtures

    public static class Declared extends Exception
    { public Declared( String msg ) { super( msg ); } }

    /** Checked, and deliberately NOT declared by Service.call -- reachable only by sneaky throw. */
    public static class Undeclared extends Exception
    { public Undeclared( String msg ) { super( msg ); } }

    public static class Unchecked extends RuntimeException
    { public Unchecked( String msg ) { super( msg ); } }

    public interface Service
    { Object call( String what ) throws Declared; }

    public static class ServiceImpl implements Service
    {
        public Object call( String what ) throws Declared
        {
            if ( "declared".equals( what ) )   throw new Declared( "declared checked" );
            if ( "unchecked".equals( what ) )  throw new Unchecked( "unchecked" );
            if ( "error".equals( what ) )      throw new AssertionError( "an Error" );
            // explicit witness: without it T infers to Throwable and javac demands it be declared
            if ( "undeclared".equals( what ) )
                ServiceImpl.<RuntimeException>sneakyThrow( new Undeclared( "undeclared checked" ) );
            return "ok";
        }

        @SuppressWarnings("unchecked")
        private static <T extends Throwable> void sneakyThrow( Throwable t ) throws T
        { throw (T) t; }
    }

    // interface inheritance, and an interface contributed only by a superclass
    public interface Base      { String base(); }
    public interface Sub extends Base { String sub(); }
    public interface FromSuper { String fromSuper(); }

    public static class Parent implements FromSuper
    { public String fromSuper() { return "parent"; } }

    public static class Child extends Parent implements Sub
    {
        public String base() { return "base"; }
        public String sub()  { return "sub"; }
    }

    /** Not public: proxies of it could be generated, but calls to them could not be dispatched. */
    interface Hidden { String hidden(); }

    public static class OnlyHidden implements Hidden
    { public String hidden() { return "hidden"; } }

    public static class PublicAndHidden implements Base, Hidden
    {
        public String base()   { return "base"; }
        public String hidden() { return "hidden"; }
    }

    public static class NoInterfaces
    { public String hello() { return "hi"; } }

    public static class NamedThing implements Base
    {
        private final String name;
        public NamedThing( String name ) { this.name = name; }
        public String base() { return name; }
        public String toString() { return "NamedThing[" + name + "]"; }
    }

    /** Reports, from inside toString, whether the caller holds the wrapper's monitor. */
    public static class LockAwareToString implements Base
    {
        public Object proxy; // assigned after wrapping
        public String base() { return "base"; }
        public String toString()
        { return String.valueOf( proxy != null && Thread.holdsLock( proxy ) ); }
    }

    // interface whose method reports whether the caller holds the proxy's lock
    public interface LockReporter { boolean callerHoldsLockOn( Object o ); }

    public static class LockReporterImpl implements LockReporter
    { public boolean callerHoldsLockOn( Object o ) { return Thread.holdsLock( o ); } }

    // ---------------------------------------------------------- convenience

    private static Service service()
    { return (Service) Synchronizer.createSynchronizedWrapper( new ServiceImpl() ); }

    /** Invokes call(what), expecting a throw, and returns whatever came out. */
    private static Throwable thrownBy( Service s, String what )
    {
        try
        {
            Object out = s.call( what );
            fail( "expected a throw for '" + what + "', got " + out );
            return null; // unreachable
        }
        catch ( Throwable t )
        { return t; }
    }

    // ------------------------------------------------------------ delegation

    public void testWrapperDelegatesNormalReturn() throws Exception
    { assertEquals( "ok", service().call( "anything-else" ) ); }

    public void testWrapperImplementsInterfacesFromWholeHierarchy()
    {
        Object p = Synchronizer.createSynchronizedWrapper( new Child() );

        assertEquals( "sub",    ((Sub) p).sub() );
        assertEquals( "an inherited super-interface should be usable", "base", ((Base) p).base() );
        assertEquals( "an interface contributed by a superclass should be usable",
                      "parent", ((FromSuper) p).fromSuper() );
    }

    /**
     *  Proxy routes equals/hashCode/toString to the InvocationHandler rather than letting
     *  them inherit Object's implementations, so the handler must deal with them explicitly.
     *  A handler that simply forwards them to the target breaks the equals contract: the
     *  proxy would evaluate target.equals(proxy), which for identity equals is false, so a
     *  wrapper would not be equal to itself.
     */
    public void testWrapperEqualsOnlyItself()
    {
        Child  target = new Child();
        Object p      = Synchronizer.createSynchronizedWrapper( target );
        Object sibling = Synchronizer.createSynchronizedWrapper( target );

        assertTrue ( "a wrapper must be equal to itself", p.equals( p ) );
        assertFalse( "a wrapper is not its target", p.equals( target ) );
        assertFalse( "and the target is not the wrapper -- symmetry holds", target.equals( p ) );
        assertFalse( "two wrappers over one target are distinct objects", p.equals( sibling ) );
        assertFalse( "equals(null) must be false, not an NPE", p.equals( null ) );
    }

    /** hashCode is identity-based, to pair correctly with the identity equals above. */
    public void testWrapperHashCodeIsIdentityBasedAndStable()
    {
        Object p = Synchronizer.createSynchronizedWrapper( new Child() );

        assertEquals( System.identityHashCode( p ), p.hashCode() );
        assertEquals( "hashCode must be stable across calls", p.hashCode(), p.hashCode() );
    }

    /**
     *  The practical payoff: a wrapper can be located and removed in the collections that
     *  hold it. Lists are the sharp case -- HashSet and HashMap test == before equals, so
     *  they mask a broken equals, while List.contains/indexOf/remove do not.
     */
    public void testWrapperBehavesInCollections()
    {
        Object p = Synchronizer.createSynchronizedWrapper( new Child() );

        java.util.List list = new java.util.ArrayList();
        list.add( p );
        assertTrue ( "List.contains should find the wrapper it holds", list.contains( p ) );
        assertEquals( 0, list.indexOf( p ) );
        assertTrue ( "List.remove should remove it", list.remove( p ) );
        assertEquals( 0, list.size() );

        java.util.Set set = new java.util.HashSet();
        set.add( p );
        set.add( p );
        assertEquals( "a wrapper added twice is one element", 1, set.size() );
        assertTrue( set.contains( p ) );

        java.util.Map map = new java.util.HashMap();
        map.put( p, "value" );
        assertEquals( "value", map.get( p ) );
    }

    /** toString reports the target rather than the proxy's default identity string. */
    public void testToStringDelegatesToTarget()
    {
        NamedThing target = new NamedThing( "the-target" );
        Object     p      = Synchronizer.createSynchronizedWrapper( target );

        assertEquals( target.toString(), p.toString() );
        assertTrue( p.toString().indexOf( "the-target" ) >= 0 );
    }

    /** ...and does so holding the wrapper's lock, since toString may read mutable state. */
    public void testToStringHoldsTheWrapperLock()
    {
        LockAwareToString target = new LockAwareToString();
        Object            p      = Synchronizer.createSynchronizedWrapper( target );
        target.proxy = p;

        assertEquals( "the proxy's monitor should be held while the target's toString runs",
                      "true", p.toString() );
    }

    /**
     *  The handler dispatches on Object's methods explicitly, and throws NoSuchMethodError
     *  if one arrives that it does not recognize. That fallback is only safe if the set of
     *  Object methods a proxy can route is exactly {equals, hashCode, toString}.
     *
     *  This checks that structurally: a proxy class can only route a method it overrides,
     *  and it can only override Object methods that are public and non-final. Every other
     *  public method on Object -- getClass, notify, notifyAll and the three waits -- is
     *  final, so it is inherited rather than overridden and never reaches the handler.
     *
     *  If a future JDK were to add a non-final public method to Object, this test fails and
     *  tells you the handler needs a new branch, rather than leaving users to discover it as
     *  a NoSuchMethodError at runtime.
     */
    public void testProxyOverridesExactlyTheThreeExpectedObjectMethods()
    {
        Object p = Synchronizer.createSynchronizedWrapper( new Child() );
        Class  proxyClass = p.getClass();

        Method[] objectMethods = Object.class.getMethods();
        List overridden = new ArrayList();
        List inherited  = new ArrayList();
        for ( int i = 0; i < objectMethods.length; ++i )
        {
            Method om = objectMethods[i];
            try
            {
                proxyClass.getDeclaredMethod( om.getName(), om.getParameterTypes() );
                if ( !overridden.contains( om.getName() ) ) overridden.add( om.getName() );
            }
            catch ( NoSuchMethodException e )
            { if ( !inherited.contains( om.getName() ) ) inherited.add( om.getName() ); }
        }
        Collections.sort( overridden );

        assertEquals( "only these Object methods may reach the handler",
                      Arrays.asList( new String[] { "equals", "hashCode", "toString" } ),
                      overridden );

        assertTrue( "everything else on Object should be inherited, not routed: " + inherited,
                    inherited.contains( "getClass" ) && inherited.contains( "wait" )
                    && inherited.contains( "notify" ) && inherited.contains( "notifyAll" ) );
    }

    /**
     *  ...and behaviorally: call every public method Object defines on a wrapper and confirm
     *  none of them trips the handler's NoSuchMethodError fallback.
     */
    public void testEveryPublicObjectMethodIsSafeToCallOnAWrapper() throws Exception
    {
        final Object p = Synchronizer.createSynchronizedWrapper( new Child() );

        // the three that ARE routed to the handler
        assertTrue( p.equals( p ) );
        p.hashCode();
        assertNotNull( p.toString() );

        // final on Object, so never routed -- exercise them anyway
        assertNotNull( p.getClass() );
        synchronized ( p )
        {
            p.notify();
            p.notifyAll();
            p.wait( 1 );
            p.wait( 1, 0 );
        }

        // the zero-argument wait() blocks, so run it on another thread and wake it.
        // entered.countDown() happens while the waiter holds p's monitor, and wait()
        // releases that monitor atomically -- so our synchronized block below cannot
        // run until the waiter is genuinely waiting, and the notify cannot be missed.
        final Throwable[] failure = new Throwable[1];
        final CountDownLatch entered = new CountDownLatch( 1 );
        Thread waiter = new Thread( "Synchronizer-wait-probe" )
        {
            public void run()
            {
                try { synchronized ( p ) { entered.countDown(); p.wait(); } }
                catch ( Throwable t ) { failure[0] = t; }
            }
        };
        waiter.setDaemon( true );
        waiter.start();
        entered.await();

        for ( int i = 0; i < 50 && waiter.isAlive(); ++i )
        {
            synchronized ( p ) { p.notifyAll(); }
            waiter.join( 100 );
        }

        assertFalse( "zero-arg wait() should have returned", waiter.isAlive() );
        assertNull( "no Object method should reach the handler's fallback, but got: " + failure[0],
                    failure[0] );
    }

    // ---------------------------------------------------------- exceptions

    /** A checked exception the interface declares must arrive exactly as thrown. */
    public void testDeclaredCheckedExceptionArrivesUnwrapped()
    {
        Throwable t = thrownBy( service(), "declared" );
        assertEquals( Declared.class, t.getClass() );
        assertEquals( "declared checked", t.getMessage() );
    }

    /** So must a RuntimeException -- this is what the wrapping defect broke most damagingly. */
    public void testUncheckedExceptionArrivesUnwrapped()
    {
        Throwable t = thrownBy( service(), "unchecked" );
        assertEquals( Unchecked.class, t.getClass() );
        assertEquals( "unchecked", t.getMessage() );
    }

    /** And an Error. */
    public void testErrorArrivesUnwrapped()
    {
        Throwable t = thrownBy( service(), "error" );
        assertEquals( AssertionError.class, t.getClass() );
    }

    /**
     *  A checked exception the interface does NOT declare must still be wrapped: the
     *  Proxy specification forbids a handler from throwing one. This is the one
     *  remaining legitimate source of UndeclaredThrowableException here, and it is
     *  reachable only by sneaky throw.
     */
    public void testUndeclaredCheckedExceptionIsWrappedAsRequiredBySpec()
    {
        Throwable t = thrownBy( service(), "undeclared" );
        assertEquals( UndeclaredThrowableException.class, t.getClass() );
        assertEquals( "the original must remain available as the cause",
                      Undeclared.class, t.getCause().getClass() );
    }

    /** Nothing should arrive wrapped in InvocationTargetException-derived layers. */
    public void testNoReflectiveWrappingLeaksThrough()
    {
        String[] cases = new String[] { "declared", "unchecked", "error" };
        for ( int i = 0; i < cases.length; ++i )
        {
            Throwable t = thrownBy( service(), cases[i] );
            assertNull( "'" + cases[i] + "' should arrive with no cause chain, got: " + t, t.getCause() );
        }
    }

    // ------------------------------------------------------- synchronization

    /** The wrapper synchronizes on itself, as documented. */
    public void testWrapperSynchronizesOnItself()
    {
        LockReporter p = (LockReporter) Synchronizer.createSynchronizedWrapper( new LockReporterImpl() );
        assertTrue( "the proxy's own monitor should be held during a call", p.callerHoldsLockOn( p ) );
    }

    /** The lock must be released when a call exits by throwing, not just by returning. */
    public void testLockIsReleasedWhenACallThrows() throws Exception
    {
        final Service s = service();

        thrownBy( s, "unchecked" ); // provoke the exception path

        final AtomicBoolean acquired = new AtomicBoolean( false );
        Thread other = new Thread()
        {
            public void run()
            { synchronized ( s ) { acquired.set( true ); } }
        };
        other.start();
        other.join( 5000 );

        assertTrue( "another thread should be able to lock the proxy after a throwing call",
                    acquired.get() );
    }

    /** Calls through one wrapper genuinely exclude one another. */
    public void testConcurrentCallsAreMutuallyExclusive() throws Exception
    {
        final AtomicInteger concurrent = new AtomicInteger( 0 );
        final AtomicInteger maxSeen    = new AtomicInteger( 0 );

        class Counter implements Base
        {
            public String base()
            {
                int now = concurrent.incrementAndGet();
                synchronized ( maxSeen )
                { if ( now > maxSeen.get() ) maxSeen.set( now ); }
                try { Thread.sleep( 5 ); }
                catch ( InterruptedException e ) { Thread.currentThread().interrupt(); }
                concurrent.decrementAndGet();
                return "base";
            }
        }

        final Base p = (Base) Synchronizer.createSynchronizedWrapper( new Counter() );

        int nThreads = 4;
        final CountDownLatch ready = new CountDownLatch( nThreads );
        final CountDownLatch go    = new CountDownLatch( 1 );
        Thread[] threads = new Thread[ nThreads ];
        for ( int i = 0; i < nThreads; ++i )
        {
            threads[i] = new Thread()
            {
                public void run()
                {
                    ready.countDown();
                    try { go.await(); } catch ( InterruptedException e ) { return; }
                    for ( int j = 0; j < 5; ++j ) p.base();
                }
            };
            threads[i].start();
        }
        ready.await();
        go.countDown();
        for ( int i = 0; i < nThreads; ++i ) threads[i].join( 30000 );

        assertEquals( "no two threads should ever be inside the target at once", 1, maxSeen.get() );
    }

    // ------------------------------------------------- public-interface rule

    /** A class with no interfaces at all fails immediately, naming itself. */
    public void testNoInterfacesFailsFast()
    {
        try
        {
            Synchronizer.createSynchronizedWrapper( new NoInterfaces() );
            fail( "expected IllegalArgumentException" );
        }
        catch ( IllegalArgumentException expected )
        {
            assertTrue( "message should name the offending class, was: " + expected.getMessage(),
                        expected.getMessage().indexOf( NoInterfaces.class.getName() ) >= 0 );
        }
    }

    /**
     *  Non-public interfaces are excluded, so a class implementing only non-public
     *  interfaces fails fast too. Previously such a proxy was created happily and then
     *  failed at the first call with UndeclaredThrowableException wrapping
     *  IllegalAccessException, because Method.invoke cannot dispatch to a non-public
     *  interface method from this package.
     */
    public void testOnlyNonPublicInterfacesFailsFast()
    {
        try
        {
            Synchronizer.createSynchronizedWrapper( new OnlyHidden() );
            fail( "expected IllegalArgumentException" );
        }
        catch ( IllegalArgumentException expected )
        {
            assertTrue( expected.getMessage().indexOf( OnlyHidden.class.getName() ) >= 0 );
        }
    }

    /** When both kinds are present, the public interfaces still work. */
    public void testPublicInterfacesSurviveAlongsideNonPublicOnes()
    {
        Object p = Synchronizer.createSynchronizedWrapper( new PublicAndHidden() );

        assertEquals( "base", ((Base) p).base() );
        assertFalse( "the non-public interface should not be implemented by the proxy",
                     Hidden.class.isInstance( p ) );
    }

    /** Every interface the proxy claims should be public. */
    public void testProxyImplementsOnlyPublicInterfaces()
    {
        Object p = Synchronizer.createSynchronizedWrapper( new PublicAndHidden() );
        Class[] ifaces = p.getClass().getInterfaces();

        assertTrue( "expected at least one interface", ifaces.length > 0 );
        for ( int i = 0; i < ifaces.length; ++i )
            assertTrue( ifaces[i] + " should be public",
                        java.lang.reflect.Modifier.isPublic( ifaces[i].getModifiers() ) );
        assertTrue( Proxy.isProxyClass( p.getClass() ) );
    }
}
