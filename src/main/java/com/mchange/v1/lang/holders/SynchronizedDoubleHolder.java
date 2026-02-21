package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedDoubleHolder implements ThreadSafeDoubleHolder
{
	 double value;

	 public synchronized double getValue()
	 { return value; }

	 public synchronized void setValue(double value)
	 { this.value = value; }
}
