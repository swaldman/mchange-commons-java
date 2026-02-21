package com.mchange.v2.holders;

import java.io.*;
import com.mchange.v2.ser.UnsupportedVersionException;

public class VolatileIntHolder implements ThreadSafeIntHolder, Serializable
{
    transient volatile int value;

    public int getValue()
    { return value; }

    public void setValue(int value)
    { this.value = value; }

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
