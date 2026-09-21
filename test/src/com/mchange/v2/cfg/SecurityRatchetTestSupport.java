package com.mchange.v2.cfg;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 *  Puts the security ratchets back into their pre-startup state, so that a test suite sharing
 *  one JVM can exercise more than one deployment's worth of configuration.
 *
 *  <p>Both ratchets are deliberately one-way. The sealed snapshot of System properties is
 *  taken once per JVM and never retaken; a boolean flag that reaches its safe value latches
 *  there and stops consulting configuration. That is the point of them, and neither offers a
 *  reset -- a seal that could be undone would not be a seal.</p>
 *
 *  <p>But a test that drives a configuration-controlled gate has to set configuration per
 *  case, which is exactly the workflow the ratchets decline to support. Each case therefore
 *  needs to look like a fresh JVM. That is what this does, reflectively, and only here: the
 *  production classes stay one-way.</p>
 *
 *  <p>Declared in com.mchange.v2.cfg so it can touch the ratchets' package-private state
 *  directly; reflection is needed only to find the fields that hold them.</p>
 */
public final class SecurityRatchetTestSupport
{
    /** Discards the sealed snapshot, so the next consultation of configuration seals afresh. */
    public static void unsealSystemProperties() throws Exception
    {
        Field f = SealedSystemProperties.class.getDeclaredField( "theProperties" );
        f.setAccessible( true );
        f.set( null, null );
    }

    /**
     *  Returns every boolean property held in a static field of the given class to the state a
     *  freshly constructed one would have.
     *
     *  <p>Matched on AbstractBooleanProperty rather than on any particular implementation.
     *  Matching a concrete class is how this quietly stopped working once the consumers moved
     *  to the sealed implementations -- those are siblings of the ratcheting ones, not
     *  subclasses, so the reset matched nothing and reported no trouble.</p>
     */
    public static void resetBooleanProperties( Class<?> holder ) throws Exception
    {
        for ( Field f : holder.getDeclaredFields() )
        {
            if ( Modifier.isStatic( f.getModifiers() )
                 && AbstractBooleanProperty.class.isAssignableFrom( f.getType() ) )
            {
                f.setAccessible( true );
                AbstractBooleanProperty p = (AbstractBooleanProperty) f.get( null );
                if ( p != null ) reset( p );
            }
        }
    }

    /** package-private state, reachable directly: we are declared in com.mchange.v2.cfg */
    private static void reset( AbstractBooleanProperty p )
    {
        p.warned.clear();
        p.warnedUnconfiguredWeakDefault = false;

        if ( p instanceof EarliestOrStrongestBooleanProperty )
            ((EarliestOrStrongestBooleanProperty) p).last = null;
        else if ( p instanceof SealedSystemPropertiesBooleanProperty )
        {
            SealedSystemPropertiesBooleanProperty sp = (SealedSystemPropertiesBooleanProperty) p;
            sp.uninitialized = true;    // note: true, not the JVM default -- this is a field initializer
            sp.sealedSys = null;
        }
        else if ( p instanceof SystemOnlyStrengthensBooleanProperty )
            ((SystemOnlyStrengthensBooleanProperty) p).systemEverStrong = false;
    }

    /**
     *  Returns every whitelist manager held in a static field of the given class to the state a
     *  freshly constructed one would have.
     *
     *  <p>Matched on WhitelistManager, for the same reason. A stateless manager needs nothing
     *  beyond its once-only warning record; a ratcheting one also has a latched whitelist.</p>
     */
    public static void resetWhitelistManagers( Class<?> holder ) throws Exception
    {
        for ( Field f : holder.getDeclaredFields() )
        {
            if ( Modifier.isStatic( f.getModifiers() )
                 && WhitelistManager.class.isAssignableFrom( f.getType() ) )
            {
                f.setAccessible( true );
                WhitelistManager wm = (WhitelistManager) f.get( null );
                if ( wm != null )
                {
                    Field ws = WhitelistManager.class.getDeclaredField( "warnedSupported" );
                    ws.setAccessible( true );
                    ws.set( wm, null );

                    if ( wm instanceof EarliestOrNarrowestWhitelistManager )
                        ((EarliestOrNarrowestWhitelistManager) wm).previousWhitelistInfo = null;
                }
            }
        }
    }

    /**
     *  Every gate a class holds, plus the sealed snapshot -- the usual need for a test class
     *  that drives configuration-controlled gates case by case.
     */
    public static void resetAll( Class<?> holder ) throws Exception
    {
        unsealSystemProperties();
        resetBooleanProperties( holder );
        resetWhitelistManagers( holder );
    }

    private SecurityRatchetTestSupport()
    {}
}
