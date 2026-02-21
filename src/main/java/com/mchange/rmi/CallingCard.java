package com.mchange.rmi;

import java.rmi.*;

public interface CallingCard
{
  public Remote  findRemote() throws ServiceUnavailableException, RemoteException;
  public boolean equals(Object o);
  public int     hashCode();
  public String  toString();
}
