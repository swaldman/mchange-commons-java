package com.mchange.v2.cfg;

import java.util.*;

class CombinedMultiPropertiesConfig extends MultiPropertiesConfig
{
    MultiPropertiesConfig[] configs;
    String[] resourcePaths;

    List parseMessages;

    CombinedMultiPropertiesConfig( MultiPropertiesConfig[] configs )
    { 
	this.configs = configs; 

	List allPaths = new LinkedList();
	
	for (int i = configs.length - 1; i >= 0; --i)
	    {
		String[] rps = configs[i].getPropertiesResourcePaths();
		for (int j = rps.length - 1; j >= 0; --j)
		    {
			String rp = rps[j];
			if (! allPaths.contains( rp ) )
			    allPaths.add(0, rp);
		    }
	    }
	this.resourcePaths = (String[]) allPaths.toArray( new String[ allPaths.size() ] );

	List pms = new LinkedList();
	for ( int i = 0, len = configs.length; i < len; ++i )
	    pms.addAll( configs[i].getDelayedLogItems() );
	this.parseMessages = Collections.unmodifiableList( pms );
    }

    private Map getPropsByResourcePaths()
    {
	Map out = new HashMap();
	for ( int i = 0, len = resourcePaths.length; i < len; ++i )
	{
	    String rp = resourcePaths[i];
	    out.put( rp, getPropertiesByResourcePath(rp) );
	}
	return Collections.unmodifiableMap( out );
    }

    public BasicMultiPropertiesConfig toBasic()
    {
	String[] rps  = getPropertiesResourcePaths();
	Map      pbrm = getPropsByResourcePaths();
	List     pms  = getDelayedLogItems();

	return new BasicMultiPropertiesConfig( rps, pbrm, pms );
    }

    public String[] getPropertiesResourcePaths()
    { return (String[]) resourcePaths.clone(); }
    
    public Properties getPropertiesByResourcePath(String path)
    {
	// Not robust to overlapping resource paths
	//
	// for (int i = configs.length - 1; i >= 0; --i)
	//     {
	// 	MultiPropertiesConfig config = configs[i];
	// 	Properties check = config.getPropertiesByResourcePath(path);
	// 	if (check != null) 
	// 	    return check;
	//     }
	// return null;

	Properties out = new Properties();
	for ( int i = 0, len = configs.length; i < len; ++i )
	    {
		MultiPropertiesConfig config = configs[i];
		Properties check = config.getPropertiesByResourcePath(path);
		if ( check != null ) out.putAll( check );
	    }
	return ( out.size() > 0 ? out : null );
    }
    
    public Properties getPropertiesByPrefix(String pfx)
    {
	List entries = new LinkedList();
	for (int i = configs.length - 1; i >= 0; --i)
	    {
		MultiPropertiesConfig config = configs[i];
		Properties check = config.getPropertiesByPrefix(pfx);
		if (check != null)
		    entries.addAll( 0, check.entrySet() );
	    }
	if (entries.size() == 0)
	    return null;
	else
	    {
		Properties out = new Properties();
		for (Iterator ii = entries.iterator(); ii.hasNext(); )
		    {
			Map.Entry entry = (Map.Entry) ii.next();
			out.put( entry.getKey(), entry.getValue() );
		    }
		return out;
	    }
    }
    
    public String getProperty( String key )
    {
	for (int i = configs.length - 1; i >= 0; --i)
	    {
		MultiPropertiesConfig config = configs[i];
		String check = config.getProperty(key);
		if (check != null) 
		    return check;
	    }
	return null;
    }

    public List getDelayedLogItems()
    { return parseMessages; }
}

