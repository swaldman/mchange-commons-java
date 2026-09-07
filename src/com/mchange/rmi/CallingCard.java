package com.mchange.rmi;

import java.rmi.*;

public interface CallingCard
{
  public Remote  findRemote() throws ServiceUnavailableException, RemoteException;
  @Override
  public boolean equals(Object o);
  @Override
  public int     hashCode();
  @Override
  public String  toString();
}
