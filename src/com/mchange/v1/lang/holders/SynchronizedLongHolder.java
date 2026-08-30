package com.mchange.v1.lang.holders;

/**
 * @deprecated use classes in com.mchange.v2.holders
 */
public class SynchronizedLongHolder implements ThreadSafeLongHolder
{
    long value;
    
    public synchronized long getValue()
    { return value; }
    
    public synchronized void setValue(long value)
    { this.value = value; }

    public SynchronizedLongHolder(long value)
    { this.value = value; }

    public SynchronizedLongHolder()
    { this(0); }
}
