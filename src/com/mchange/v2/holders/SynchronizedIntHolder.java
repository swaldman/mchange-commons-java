package com.mchange.v2.holders;

import java.io.*;
import com.mchange.v2.ser.UnsupportedVersionException;

public class SynchronizedIntHolder implements ThreadSafeIntHolder, Serializable
{
    transient int value;

    public SynchronizedIntHolder( int value )
    { this.value = value; }

    public SynchronizedIntHolder()
    { this(0); }

    public synchronized int getValue()
    { return value; }

    public synchronized void setValue(int value)
    { this.value = value; }

    public synchronized void increment()
    { ++value; }

    public synchronized void decrement()
    { --value; }

    //Serialization
    static final long serialVersionUID = 1; //override to take control of versioning
    private final static short VERSION = 0x0001;
    
    private void writeObject(ObjectOutputStream out) throws IOException
    {
	out.writeShort(VERSION);
	out.writeInt(value);
    }
    
    private void readObject(ObjectInputStream in) throws IOException
    {
	short version = in.readShort();
	switch (version)
	    {
	    case 0x0001:
		this.value = in.readInt();
		break;
	    default:
		throw new UnsupportedVersionException(this, version);
	    }
    }
}
