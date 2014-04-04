/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
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
