/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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
