package com.mchange.util;

public class PartialFailureException extends Exception
{
  public PartialFailureException(String message)
    {super(message);}

  public PartialFailureException()
    {super();}
}
