package com.mchange.v1.lang.holders;

/**
 * An interface for thread-safe wrappers for a mutable boolean.
 *
 * @see SynchronizedBooleanHolder
 * @see VolatileBooleanHolder
 *
 * @deprecated use classes in com.mchange.v2.holders
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
