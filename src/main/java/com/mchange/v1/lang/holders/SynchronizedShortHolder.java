package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedShortHolder implements ThreadSafeShortHolder
{
	 short value;

	 public synchronized short getValue()
	 { return value; }

	 public synchronized void setValue(short value)
	 { this.value = value; }
}
