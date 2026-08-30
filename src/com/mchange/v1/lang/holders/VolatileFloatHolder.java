package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class VolatileFloatHolder implements ThreadSafeFloatHolder
{
	 volatile float value;

	 public float getValue()
	 { return value; }

	 public void setValue(float value)
	 { this.value = value; }
}
