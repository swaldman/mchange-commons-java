package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class VolatileByteHolder implements ThreadSafeByteHolder
{
	 volatile byte value;

	 @Override
	 public byte getValue()
	 { return value; }

	 @Override
	 public void setValue(byte value)
	 { this.value = value; }
}
