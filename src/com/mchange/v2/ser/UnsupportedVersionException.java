package com.mchange.v2.ser;

import java.io.*;

public class UnsupportedVersionException extends InvalidClassException
{
  public UnsupportedVersionException(String message)
  {super(message);}

  public UnsupportedVersionException(Object obj, int version)
  {this(obj.getClass().getName() + " -- unsupported version: " + version);}
}
