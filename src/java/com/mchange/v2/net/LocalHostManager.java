/*
 * Distributed as part of mchange-commons-java v.0.2.3.3
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

package com.mchange.v2.net;

import java.net.*;
import java.util.*;

public class LocalHostManager
{
    Set localAddresses;
    Set knownGoodNames;
    Set knownBadNames;

    public synchronized void update() throws SocketException
    {
	Set tmp = new HashSet();
	Enumeration netintfcs = NetworkInterface.getNetworkInterfaces();
	while (netintfcs.hasMoreElements())
	    {
		NetworkInterface ni = (NetworkInterface) netintfcs.nextElement();
		Enumeration addresses = ni.getInetAddresses();
		while (addresses.hasMoreElements())
		    tmp.add( addresses.nextElement() );
	    }
	this.localAddresses = Collections.unmodifiableSet( tmp );
	this.knownGoodNames = new HashSet();
	this.knownBadNames = new HashSet();
    }

    public synchronized Set getLocalAddresses()
    { return localAddresses; }

    public synchronized boolean isLocalAddress(InetAddress addr)
    { return localAddresses.contains( addr ); }

    public synchronized boolean isLocalHostName( String name ) 
    {
	if ( knownGoodNames.contains( name ) )
	    return true;
	else if ( knownGoodNames.contains( name ) )
	    return false;
	else
	    {
		try
		    {
			InetAddress nameAddr = InetAddress.getByName( name );
			if ( localAddresses.contains( nameAddr ) )
			    {
				knownGoodNames.add( name );
				return true;
			    }
			else
			    {
				knownBadNames.add( name );
				return false;
			    }
		    }
		catch (UnknownHostException e)
		    {
			knownBadNames.add( name );
			return false;
		    }
	    }
    }

    

    public LocalHostManager() throws SocketException
    { update(); }

    public static void main( String[] argv )
    {
	try
	    {
		LocalHostManager lhm = new LocalHostManager();
		System.out.println( lhm.getLocalAddresses() );
	    }
	catch (Exception e)
	    { e.printStackTrace(); }
    }
}
