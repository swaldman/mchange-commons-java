/*
 * Distributed as part of mchange-commons-java v.0.2.4
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
import java.net.*;
import java.rmi.*;
import java.rmi.registry.*;
import com.mchange.io.*;

public class RMIRegistryCallingCard implements CallingCard, Serializable
{
  transient Remote cached = null; //really transient

  transient /* not */ String url;

  public RMIRegistryCallingCard(String regHost, int reg_port, String name)
    {this.url = "//" + regHost.toLowerCase() + ':' + reg_port + '/' + name;}

  public RMIRegistryCallingCard(String regHost, String name)
    {this(regHost, Registry.REGISTRY_PORT, name);}

  public boolean equals(Object o)
    {return (o instanceof RMIRegistryCallingCard) && this.url.equals(((RMIRegistryCallingCard) o).url);}

  public int hashCode()
    {return url.hashCode();}

  public Remote findRemote() throws ServiceUnavailableException, RemoteException
    {
      if (cached instanceof Checkable)
	{
	  try
	    {
	      ((Checkable) cached).check();
	      return cached;
	    }
	  catch (RemoteException e)
	    {
	      cached = null;
	      return findRemote();
	    }
	}
      else
	{
	  try
	    {
	      Remote r = Naming.lookup(url);
	      if (r instanceof Checkable)
		cached = r;
	      return r;
	    }
	  catch (NotBoundException e)
	    {throw new ServiceUnavailableException("Object Not Bound: " + url);}
	  catch (MalformedURLException e) //I'd like to check for this in constructor, but how?
	    {throw new ServiceUnavailableException("Uh oh. Bad url. It never will be available: " + url);}
	}
    }

  public String toString()
    {return super.toString() + " [" + url + "];";}

  //Serialization stuff
  static final long serialVersionUID = 1;
  private final static short VERSION = 0x0001;
  
  private void writeObject(ObjectOutputStream out) throws IOException
    {
      out.writeShort(VERSION);
      
      out.writeUTF(url);
    }
  
  private void readObject(ObjectInputStream in) throws IOException
    {
      short version = in.readShort();
      switch (version)
	{
	case 0x0001:
	  url = in.readUTF();
	  break;
	default:
	  throw new UnsupportedVersionException(this.getClass().getName() + "; Bad version: " + version);
	}
    }
}
  
