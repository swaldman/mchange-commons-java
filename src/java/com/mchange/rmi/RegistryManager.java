/*
 * Distributed as part of mchange-commons-java v.0.2.1
 *
 * Copyright (C) 2012 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2.1, as 
 * published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; see the file LICENSE.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */


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
