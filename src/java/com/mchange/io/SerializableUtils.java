/*
 * Distributed as part of mchange-commons-java v.0.2.1
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


package com.mchange.io;

import java.io.*;

/**
 * @deprecated use com.mchange.v2.ser.SerializableUtils
 */
public final class SerializableUtils
{
  public final static Object unmarshallObjectFromFile(File file) throws IOException, ClassNotFoundException
    {
      ObjectInputStream in = null;
      try
	{
	  in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
	  return in.readObject();
	}
      finally
	{InputStreamUtils.attemptClose(in);}
    }

  public final static void marshallObjectToFile(Object o, File file) throws IOException
    {
      ObjectOutputStream out = null;
      try
	{
	  out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	  out.writeObject(o);
	}
      finally
	{OutputStreamUtils.attemptClose(out);}
    }

  private SerializableUtils()
    {}
}
