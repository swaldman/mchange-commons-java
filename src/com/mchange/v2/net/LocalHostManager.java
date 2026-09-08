package com.mchange.v2.net;

import java.net.*;
import java.util.*;

public class LocalHostManager
{
    Set<InetAddress> localAddresses;
    Set<String> knownGoodNames;
    Set<String> knownBadNames;

    public synchronized void update() throws SocketException
    {
	Set<InetAddress> tmp = new HashSet<InetAddress>();
	Enumeration<NetworkInterface> netintfcs = NetworkInterface.getNetworkInterfaces();
	while (netintfcs.hasMoreElements())
	    {
		NetworkInterface ni = netintfcs.nextElement();
		Enumeration<InetAddress> addresses = ni.getInetAddresses();
		while (addresses.hasMoreElements())
		    tmp.add( addresses.nextElement() );
	    }
	this.localAddresses = Collections.unmodifiableSet( tmp );
	this.knownGoodNames = new HashSet<String>();
	this.knownBadNames = new HashSet<String>();
    }

    public synchronized Set<InetAddress> getLocalAddresses()
    { return localAddresses; }

    public synchronized boolean isLocalAddress(InetAddress addr)
    { return localAddresses.contains( addr ); }

    public synchronized boolean isLocalHostName( String name ) 
    {
	if ( knownGoodNames.contains( name ) )
	    return true;
	else if ( knownBadNames.contains( name ) )
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
