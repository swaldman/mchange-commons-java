package com.mchange.io;

import java.io.*;

public class UnknownStreamClassException extends InvalidClassException
{
  public UnknownStreamClassException(ClassNotFoundException e)
  {super(e.getMessage());}
}
