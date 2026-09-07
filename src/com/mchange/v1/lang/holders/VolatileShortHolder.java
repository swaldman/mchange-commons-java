package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class VolatileShortHolder implements ThreadSafeShortHolder
{
	 volatile short value;

	 @Override
	 public short getValue()
	 { return value; }

	 @Override
	 public void setValue(short value)
	 { this.value = value; }
}
