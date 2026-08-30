package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class VolatileCharHolder implements ThreadSafeCharHolder
{
	 volatile char value;

	 public char getValue()
	 { return value; }

	 public void setValue(char value)
	 { this.value = value; }
}
