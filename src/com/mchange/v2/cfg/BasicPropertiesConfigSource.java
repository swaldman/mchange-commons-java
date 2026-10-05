package com.mchange.v2.cfg;

import java.util.*;
import java.io.*;

import java.net.URL;

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
        {
            // only in the case where we literally found no resource paths, and no message would be lost
            String message = String.format("No properties could be loaded from classloader resource path '%s'.", identifier);
	    throw new FileNotFoundException( message );
        }
    }

    private Properties propertiesForIdentifier(String identifier, List<DelayedLogItem> delayedLogItemsOut) throws IOException
    {
        // ClassLoader.getResources() always treats paths as absolute, unlike Class.getResourceAsStream(),
        // which we formerly used, for which a leading slash meant an absolute path from ClassLoader root,
        // and other paths would be treated as relative to the location of the Class.
        //
        // we require properties resources (as opposed to other potential config sources)
        // to be identified by leading slashes, so we need to strip.
        String resourcePath;
        if (!identifier.startsWith("/")) // programming error, only slash-beginning identifiers should be delegate to this class
            throw new IllegalArgumentException("BasicPropertiesConfigSource expects identifiers beginning with '/', found '" + identifier + "'.");
        else
            resourcePath = identifier.substring(1);

        ArrayList<URL> allResources = Collections.list(BasicPropertiesConfigSource.class.getClassLoader().getResources(resourcePath));

        int sz = allResources.size();
        Properties out = null;

        if (sz > 0)
        {
            out = new Properties();

            // we want earlier entries in the CLASSPATH to have priority, so we start
            // at the last element and overwrite as we work backwards
            for (int i = sz; --i >= 0;) 
            {
                URL resource = allResources.get(i);
                InputStream pis = new BufferedInputStream( resource.openStream() );
                try
                {
                    Properties tmp = new Properties();
                    tmp.load(pis);
                    Set<String> overwrites = new HashSet<>(tmp.stringPropertyNames());
                    overwrites.retainAll(out.keySet());
                    if (overwrites.size() > 0)
                    {
                        String message = "Resource '" + resource + "' overrides the following keys " + overwrites + " defined in a resource later, and therefore lower priority, in list " + allResources + ".";
                        delayedLogItemsOut.add(new DelayedLogItem(Level.WARNING, message));
                    }
                    out.putAll( tmp );
                }
                catch (Exception e)
                    { delayedLogItemsOut.add(new DelayedLogItem(Level.WARNING, "An Exception occurred while trying to load an expected property file resource from '" + resource + "'.", e)); }
                finally
                {
                    try { if ( pis != null ) pis.close(); } //ensures closure of nested rawStream as well
                    catch (IOException e) 
                        { delayedLogItemsOut.add( new DelayedLogItem( Level.WARNING, "An IOException occurred while closing InputStream from resource path '" + identifier + "'.", e ) ); }
                }
            }
        }

        return out;
    }
}

