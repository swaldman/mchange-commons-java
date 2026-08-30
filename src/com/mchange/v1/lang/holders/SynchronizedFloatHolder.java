package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedFloatHolder implements ThreadSafeFloatHolder
{
	 float value;

	 public synchronized float getValue()
	 { return value; }

	 public synchronized void setValue(float value)
	 { this.value = value; }
}
