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
