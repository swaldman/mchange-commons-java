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
