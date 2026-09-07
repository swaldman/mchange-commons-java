package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedCharHolder implements ThreadSafeCharHolder
{
	 char value;

	 @Override
	 public synchronized char getValue()
	 { return value; }

	 @Override
	 public synchronized void setValue(char value)
	 { this.value = value; }
}
