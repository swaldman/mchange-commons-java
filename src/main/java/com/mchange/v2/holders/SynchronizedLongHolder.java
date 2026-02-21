package com.mchange.v2.holders;

import java.io.*;
import com.mchange.v2.ser.UnsupportedVersionException;

public class SynchronizedLongHolder implements ThreadSafeLongHolder, Serializable
{
    transient long value;
    
    public synchronized long getValue()
    { return value; }
    
    public synchronized void setValue(long value)
    { this.value = value; }

    public SynchronizedLongHolder(long value)
    { this.value = value; }

    public SynchronizedLongHolder()
    { this(0); }


    //Serialization
    static final long serialVersionUID = 1; //override to take control of versioning
    private final static short VERSION = 0x0001;
    
    private void writeObject(ObjectOutputStream out) throws IOException
    {
	out.writeShort(VERSION);
	out.writeLong(value);
    }
    
    private void readObject(ObjectInputStream in) throws IOException
    {
	short version = in.readShort();
	switch (version)
	    {
	    case 0x0001:
		this.value = in.readLong();
		break;
	    default:
		throw new UnsupportedVersionException(this, version);
	    }
    }
}
