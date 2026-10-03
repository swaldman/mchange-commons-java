package com.mchange.v2.cfg;

import java.util.*;
import java.io.*;

import static com.mchange.v2.cfg.DelayedLogItem.*;

public final class BasicPropertiesConfigSource implements PropertiesConfigSource
{
    @Override
    public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception
    {
        List<DelayedLogItem> messages = new LinkedList<DelayedLogItem>();
        Properties props = propertiesForIdentifier( identifier, messages );
        if (props != null)
	    return new Parse(props, messages);
	else
	    throw new FileNotFoundException( String.format("Resource not found at path '%s'.", identifier) );
    }

    private Properties propertiesForIdentifier(String identifier, List<DelayedLogItem> delayedLogItemsOut) throws IOException
    {
        Properties out = null;
	InputStream rawStream = MultiPropertiesConfig.class.getResourceAsStream( identifier );
	if ( rawStream != null )
	{
	    InputStream pis = new BufferedInputStream( rawStream );
	    out = new Properties();
	    try
	    { out.load( pis ); }
	    finally
	    {
		try { if ( pis != null ) pis.close(); } //ensures closure of nested rawStream as well
		catch (IOException e) 
		    { delayedLogItemsOut.add( new DelayedLogItem( Level.WARNING, "An IOException occurred while closing InputStream from resource path '" + identifier + "'.", e ) ); }
	    }
        }
        else
        {
            // this message would be lost in practice for now, since we currently throw when out is null
            //delayedLogItemsOut.add( new DelayedLogItem( Level.FINER, "No resource found at resource path '" + identifier + "'." ) );
        }
        return out;
    }
}

