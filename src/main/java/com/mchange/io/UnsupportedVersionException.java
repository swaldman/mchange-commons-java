package com.mchange.io;

import java.io.*;


/**
 * @deprecated use com.mchange.v2.ser.UnsupportedVersionException
 */
public class UnsupportedVersionException extends InvalidClassException
{
  public UnsupportedVersionException(String message)
  {super(message);}

  public UnsupportedVersionException(Object obj, int version)
  {this(obj.getClass().getName() + " -- unsupported version: " + version);}
}
