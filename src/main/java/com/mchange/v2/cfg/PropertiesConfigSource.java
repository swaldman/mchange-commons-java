package com.mchange.v2.cfg;

import java.util.*;
import java.io.FileNotFoundException;

/**
 * implementations should have no-arg constructors
 */
public interface PropertiesConfigSource
{
    /**
     *  An Exception signifies this source cannot be parsed at all;
     *  it is a bad source. More local failures should be handled and
     *  reported in parse messages.
     */
    public Parse propertiesFromSource( String identifier ) throws FileNotFoundException, Exception;

    public static class Parse
    {
	private Properties           properties;
	private List<DelayedLogItem> parseMessages;

	public Properties           getProperties()    { return properties; }
	public List<DelayedLogItem> getDelayedLogItems() { return parseMessages; }

	public Parse( Properties properties, List<DelayedLogItem> parseMessages )
	{
	    this.properties    = properties;
	    this.parseMessages = parseMessages;
	}
    }
}
