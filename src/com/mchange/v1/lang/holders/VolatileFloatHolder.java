package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class VolatileFloatHolder implements ThreadSafeFloatHolder
{
	 volatile float value;

	 @Override
	 public float getValue()
	 { return value; }

	 @Override
	 public void setValue(float value)
	 { this.value = value; }
}
