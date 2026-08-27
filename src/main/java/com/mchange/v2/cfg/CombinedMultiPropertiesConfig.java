package com.mchange.v2.cfg;

import java.util.*;

class CombinedMultiPropertiesConfig extends MultiPropertiesConfig
{
    MultiPropertiesConfig[] configs;
    String[] resourcePaths;

    List parseMessages;

    Set allRead;
    Set allVetoed;
    Set allNotFound;
    Set allFaults;

    CombinedMultiPropertiesConfig( MultiPropertiesConfig[] configs )
    { 
	this.configs = configs; 

	List allPaths = new LinkedList();

	for (int i = configs.length - 1; i >= 0; --i)
	    {
		String[] rps = ConfigUtils.nullFilter(configs[i].getPropertiesResourcePaths()); // there should be no null values, but out of an abundance of caution
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

        this.allRead = Collections.unmodifiableSet(new HashSet(Arrays.asList(resourcePaths)));

        Set af = new HashSet();
	for ( int i = 0, len = configs.length; i < len; ++i )
	    af.addAll( configs[i].getAllFaults() );
        af.removeAll(allRead);

        Set anf = new HashSet();
	for ( int i = 0, len = configs.length; i < len; ++i )
	    anf.addAll( configs[i].getAllNotFound() );
        anf.removeAll(allRead);
        anf.removeAll(af);

        Set av = new HashSet();
	for ( int i = 0, len = configs.length; i < len; ++i )
	    av.addAll( configs[i].getAllVetoed() );
        av.removeAll(allRead);
        av.removeAll(af);
        av.removeAll(anf);

        this.allFaults   = Collections.unmodifiableSet(af);
        this.allNotFound = Collections.unmodifiableSet(anf);
        this.allVetoed   = Collections.unmodifiableSet(av);
    }

    private Map getPropsByResourcePaths()
    {
	Map out = new HashMap();
	for ( int i = 0, len = resourcePaths.length; i < len; ++i )
	{
	    String rp = resourcePaths[i];
            Properties props = this.getPropertiesByResourcePath(rp);
	    out.put( rp, props == null ? new Properties() : props );
	}
	return Collections.unmodifiableMap( out );
    }

    public BasicMultiPropertiesConfig toBasic()
    {
	String[] rps  = getPropertiesResourcePaths();
	Map      pbrm = getPropsByResourcePaths();
	List     pms  = getDelayedLogItems();

	return new BasicMultiPropertiesConfig( rps, pbrm, pms, allVetoed, allNotFound, allFaults );
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
	return out;
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
        Properties out = new Properties();
        for (Iterator ii = entries.iterator(); ii.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) ii.next();
                out.put( entry.getKey(), entry.getValue() );
            }
        return out;
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

    public boolean wasRead(String resourcePath)
    { return allRead.contains(resourcePath); }

    public boolean wasVetoed(String resourcePath)
    { return allVetoed.contains(resourcePath); }

    public boolean wasNotFound(String resourcePath)
    { return allNotFound.contains(resourcePath); }

    public boolean wasFault(String resourcePath)
    { return allFaults.contains(resourcePath); }

    public Set getAllRead()
    { return allRead; }

    public Set getAllVetoed()
    { return allVetoed; }

    public Set getAllNotFound()
    { return allNotFound; }

    public Set getAllFaults()
    { return allFaults; }
}

