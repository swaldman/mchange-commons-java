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
