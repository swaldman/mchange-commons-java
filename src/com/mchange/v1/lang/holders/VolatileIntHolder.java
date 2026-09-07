package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class VolatileIntHolder implements ThreadSafeIntHolder
{
	 volatile int value;

	 @Override
	 public int getValue()
	 { return value; }

	 @Override
	 public void setValue(int value)
	 { this.value = value; }
}
