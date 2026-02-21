package com.mchange.v2.holders;

/**
 * An interface for thread-safe wrappers for a mutable boolean.
 *
 * @see SynchronizedBooleanHolder
 * @see VolatileBooleanHolder
 */
public interface ThreadSafeBooleanHolder
{
    /**
     * gets the value of the wrapped boolean
     */
    public boolean getValue();

    /**
     * sets the value of the wrapped boolean
     */
    public void setValue(boolean b);
}
