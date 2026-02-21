package com.mchange.v1.lang.holders;

/**
 * An implementation of ThreadSafeBooleanHolder that
 * synchronizes on itself for all acesses and mutations 
 * of the underlying boolean.
 *
 * @see VolatileBooleanHolder
 *
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedBooleanHolder implements ThreadSafeBooleanHolder
{
	 boolean value;

	 public synchronized boolean getValue()
	 { return value; }

	 public synchronized void setValue(boolean b)
	 { this.value = b; }
}
