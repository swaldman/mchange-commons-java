package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class VolatileBooleanHolder implements ThreadSafeBooleanHolder
{
	 volatile boolean value;

	 public boolean getValue()
	 { return value; }

	 public void setValue(boolean value)
	 { this.value = value; }
}
