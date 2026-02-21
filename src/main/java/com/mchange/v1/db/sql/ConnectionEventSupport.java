package com.mchange.v1.db.sql;

import java.util.*;
import java.sql.*;
import javax.sql.*;

/**
 * @deprecated use com.mchange.v2.c3p0.util.ConectionEventSupport
 */
public class ConnectionEventSupport
{
    PooledConnection source;
    Set              mlisteners = new HashSet();

    public ConnectionEventSupport(PooledConnection source)
    { this.source = source; }

    public synchronized void addConnectionEventListener(ConnectionEventListener mlistener)
    {mlisteners.add(mlistener);}

    public synchronized void removeConnectionEventListener(ConnectionEventListener mlistener)
    {mlisteners.remove(mlistener);}

    public synchronized void fireConnectionClosed()
    {
	ConnectionEvent evt = new ConnectionEvent(source);
	for (Iterator i = mlisteners.iterator(); i.hasNext();)
	    {
		ConnectionEventListener cl = (ConnectionEventListener) i.next();
		cl.connectionClosed(evt);
	    }
    }

    public synchronized void fireConnectionErrorOccurred(SQLException error)
    {
	ConnectionEvent evt = new ConnectionEvent(source, error);
	for (Iterator i = mlisteners.iterator(); i.hasNext();)
	    {
		ConnectionEventListener cl = (ConnectionEventListener) i.next();
		cl.connectionErrorOccurred(evt);
	    }
    }
}



