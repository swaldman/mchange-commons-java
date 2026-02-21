package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class VolatileByteHolder implements ThreadSafeByteHolder
{
	 volatile byte value;

	 public byte getValue()
	 { return value; }

	 public void setValue(byte value)
	 { this.value = value; }
}
