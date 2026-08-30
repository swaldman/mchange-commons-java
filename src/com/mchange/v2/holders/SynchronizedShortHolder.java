package com.mchange.v2.holders;

import java.io.*;
import com.mchange.v2.ser.UnsupportedVersionException;

public class SynchronizedShortHolder implements ThreadSafeShortHolder, Serializable
{
    transient short value;

    public synchronized short getValue()
    { return value; }

    public synchronized void setValue(short value)
    { this.value = value; }


    //Serialization
    static final long serialVersionUID = 1; //override to take control of versioning
    private final static short VERSION = 0x0001;
    
    private void writeObject(ObjectOutputStream out) throws IOException
    {
	out.writeShort(VERSION);
	out.writeShort(value);
    }
    
    private void readObject(ObjectInputStream in) throws IOException
    {
	short version = in.readShort();
	switch (version)
	    {
	    case 0x0001:
		this.value = in.readShort();
		break;
	    default:
		throw new UnsupportedVersionException(this, version);
	    }
    }
}
