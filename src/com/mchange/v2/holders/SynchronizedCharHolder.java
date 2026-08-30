package com.mchange.v2.holders;

import java.io.*;
import com.mchange.v2.ser.UnsupportedVersionException;

public class SynchronizedCharHolder implements ThreadSafeCharHolder, Serializable
{
    transient char value;

    public synchronized char getValue()
    { return value; }

    public synchronized void setValue(char value)
    { this.value = value; }


    //Serialization
    static final long serialVersionUID = 1; //override to take control of versioning
    private final static short VERSION = 0x0001;
    
    private void writeObject(ObjectOutputStream out) throws IOException
    {
	out.writeShort(VERSION);
	out.writeChar(value);
    }
    
    private void readObject(ObjectInputStream in) throws IOException
    {
	short version = in.readShort();
	switch (version)
	    {
	    case 0x0001:
		this.value = in.readChar();
		break;
	    default:
		throw new UnsupportedVersionException(this, version);
	    }
    }
}
