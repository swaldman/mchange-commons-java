package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedShortHolder implements ThreadSafeShortHolder
{
	 short value;

	 @Override
	 public synchronized short getValue()
	 { return value; }

	 @Override
	 public synchronized void setValue(short value)
	 { this.value = value; }
}
