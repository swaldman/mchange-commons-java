package com.mchange.lang;

import java.io.*;

/**
 * @deprecated use com.mchange.v2.ser.SerializableUtils;
 */
public final class ObjectUtils
{
  private ObjectUtils()
    {}

    public final static Object DUMMY_OBJECT = new Object();

    //Object must be Serializable
    public static byte[] objectToByteArray(Object obj) throws NotSerializableException
    {
	try
	{
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream    out  = new ObjectOutputStream(baos);
	    out.writeObject(obj);
	    return baos.toByteArray();
	}
	catch (NotSerializableException e)
	{
	    //this is the only IOException that 
	    //shouldn't signal a bizarre error...
	    e.fillInStackTrace();
	    throw e;
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	    throw new Error("IOException writing to a byte array!");
	}
    }
    
    //the byte array is presumend to be a Serialized object
    public static Object objectFromByteArray(byte[] bytes) throws IOException, ClassNotFoundException
    {
	ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
	return in.readObject();
    }
}
