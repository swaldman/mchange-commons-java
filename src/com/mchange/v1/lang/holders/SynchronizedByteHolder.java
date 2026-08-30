package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedByteHolder implements ThreadSafeByteHolder
{
	 byte value;

	 public synchronized byte getValue()
	 { return value; }

	 public synchronized void setValue(byte b)
	 { this.value = b; }
}
