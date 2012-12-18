/*
 * Distributed as part of mchange-commons-java v.0.2.3.3
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


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
