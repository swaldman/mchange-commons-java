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
     *  Releases every EarliestOrStrongestBooleanProperty held in a static field of the given
     *  class, so a flag latched at its safe value will consult configuration again.
     *
     *  <p>Also clears each one's record of what it has already complained about, so a case
     *  asserting on a warning is not silenced by an earlier case having provoked it.</p>
     */
    public static void resetBooleanRatchets( Class<?> holder ) throws Exception
    {
        for ( Field f : holder.getDeclaredFields() )
        {
            if ( Modifier.isStatic( f.getModifiers() )
                 && EarliestOrStrongestBooleanProperty.class.isAssignableFrom( f.getType() ) )
            {
                f.setAccessible( true );
                EarliestOrStrongestBooleanProperty p = (EarliestOrStrongestBooleanProperty) f.get( null );
                if ( p != null )
                {
                    p.last = null;          // package-private: we are in com.mchange.v2.cfg
                    p.warned.clear();
                }
            }
        }
    }

    /** Both of the above, for the common case of a class whose gates are all config-driven. */
    public static void resetAll( Class<?> holder ) throws Exception
    {
        unsealSystemProperties();
        resetBooleanRatchets( holder );
    }

    private SecurityRatchetTestSupport()
    {}
}
