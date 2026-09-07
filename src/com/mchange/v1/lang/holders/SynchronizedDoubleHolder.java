package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedDoubleHolder implements ThreadSafeDoubleHolder
{
	 double value;

	 @Override
	 public synchronized double getValue()
	 { return value; }

	 @Override
	 public synchronized void setValue(double value)
	 { this.value = value; }
}
