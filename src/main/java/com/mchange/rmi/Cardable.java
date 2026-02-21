package com.mchange.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Cardable extends Remote
{
  public CallingCard getCallingCard()
    throws ServiceUnavailableException, RemoteException;
}
