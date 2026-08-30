package com.mchange.rmi;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

public class RegistryManager
{
  public static Registry ensureRegistry(int port) throws RemoteException
    {
      Registry reg = findRegistry(port);
      if (reg == null) reg = LocateRegistry.createRegistry(port);
      return reg;
    }

  public static Registry ensureRegistry() throws RemoteException
    {return ensureRegistry(Registry.REGISTRY_PORT);}

  public static boolean registryAvailable(int port) throws RemoteException, AccessException
    {
      try
	{
	  Registry reg = LocateRegistry.getRegistry(port);
	  reg.list(); //just a safe registry call, to see if the registry exists
	  return true;
	}
      catch (java.rmi.ConnectException e) //this is what we get if no Registry is exported
	{return false;}
    }

  public static boolean registryAvailable() throws RemoteException, AccessException
    {return registryAvailable(Registry.REGISTRY_PORT);}

  /**
   * @return the Registry on localhost at port, if one is exported; null otherwise.
   */
  public static Registry findRegistry(int port) throws RemoteException, AccessException
    {
      if (!registryAvailable(port)) return null;
      else return LocateRegistry.getRegistry(port);
    }

  /**
   * @return the Registry on localhost at the default registry port, if one is exported; null otherwise. 
   */
  public static Registry findRegistry() throws RemoteException, AccessException
    {return findRegistry(Registry.REGISTRY_PORT);}
}
