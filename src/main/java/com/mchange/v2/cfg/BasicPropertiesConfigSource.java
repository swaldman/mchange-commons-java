package com.mchange.v2.cfg;

import java.util.*;
import java.io.*;

import static com.mchange.v2.cfg.DelayedLogItem.*;

public final class BasicPropertiesConfigSource implements PropertiesConfigSource
{
    public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception
    {
	InputStream rawStream = MultiPropertiesConfig.class.getResourceAsStream( identifier );
	if ( rawStream != null )
	{
	    InputStream pis = new BufferedInputStream( rawStream );
	    Properties p = new Properties();
	    List<DelayedLogItem> messages = new LinkedList<DelayedLogItem>();
	    try
	    { p.load( pis ); }
	    finally
	    {
		try { if ( pis != null ) pis.close(); } //ensures closuer of nested rawStream as well
		catch (IOException e) 
		    { messages.add( new DelayedLogItem( Level.WARNING, "An IOException occurred while closing InputStream from resource path '" + identifier + "'.", e ) ); }
	    }
	    return new Parse(p, messages);
	}
	else
	    throw new FileNotFoundException( String.format("Resource not found at path '%s'.", identifier) );
    }
}

