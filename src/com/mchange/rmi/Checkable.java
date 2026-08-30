package com.mchange.rmi;

import java.rmi.*;

public interface Checkable extends Remote
{
  public void check() throws ServiceUnavailableException, RemoteException;
}
