package com.mchange.rmi;

public class NotAuthorizedException extends Exception
{
  public NotAuthorizedException()
    {super();}

  public NotAuthorizedException(String msg)
    {super(msg);}
}
