package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedIntHolder implements ThreadSafeIntHolder
{
	 int value;

	 @Override
	 public synchronized int getValue()
	 { return value; }

	 @Override
	 public synchronized void setValue(int value)
	 { this.value = value; }
}
