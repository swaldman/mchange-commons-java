package com.mchange.v2.holders;

import java.io.*;
import com.mchange.v2.ser.UnsupportedVersionException;

/**
 * An implementation of ThreadSafeBooleanHolder that
 * synchronizes on itself for all acesses and mutations 
 * of the underlying boolean.
 *
 * @see VolatileBooleanHolder
 */
public class SynchronizedBooleanHolder implements ThreadSafeBooleanHolder, Serializable
{
    transient boolean value;

    public synchronized boolean getValue()
    { return value; }

    public synchronized void setValue(boolean b)
    { this.value = b; }


    //Serialization
    static final long serialVersionUID = 1; //override to take control of versioning
    private final static short VERSION = 0x0001;
    
    private void writeObject(ObjectOutputStream out) throws IOException
    {
	out.writeShort(VERSION);
	out.writeBoolean(value);
    }
    
    private void readObject(ObjectInputStream in) throws IOException
    {
	short version = in.readShort();
	switch (version)
	    {
	    case 0x0001:
		this.value = in.readBoolean();
		break;
	    default:
		throw new UnsupportedVersionException(this, version);
	    }
    }
}
