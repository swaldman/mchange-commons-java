package com.mchange.v2.cfg;

import java.util.*;

class CombinedMultiPropertiesConfig extends MultiPropertiesConfig
{
    MultiPropertiesConfig[] configs;
    String[] resourcePaths;

    List<DelayedLogItem> parseMessages;

    Set<String> allRead;
    Set<String> allVetoed;
    Set<String> allNotFound;
    Set<String> allFaults;

    CombinedMultiPropertiesConfig( MultiPropertiesConfig[] configs )
    { 
	this.configs = configs; 

	List<String> allPaths = new LinkedList<String>();

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

	List<DelayedLogItem> pms = new LinkedList<DelayedLogItem>();
	for ( int i = 0, len = configs.length; i < len; ++i )
	    pms.addAll( configs[i].getDelayedLogItems() );
	this.parseMessages = Collections.unmodifiableList( pms );

        this.allRead = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(resourcePaths)));

        Set<String> av = new HashSet<String>();
	for ( int i = 0, len = configs.length; i < len; ++i )
	    av.addAll( configs[i].getAllVetoed() );
        av.removeAll(allRead);

        Set<String> af = new HashSet<String>();
	for ( int i = 0, len = configs.length; i < len; ++i )
	    af.addAll( configs[i].getAllFaults() );
        af.removeAll(allRead);
        af.removeAll(av);

        Set<String> anf = new HashSet<String>();
	for ( int i = 0, len = configs.length; i < len; ++i )
	    anf.addAll( configs[i].getAllNotFound() );
        anf.removeAll(allRead);
        anf.removeAll(av);
        anf.removeAll(af);

        this.allFaults   = Collections.unmodifiableSet(af);
        this.allNotFound = Collections.unmodifiableSet(anf);
        this.allVetoed   = Collections.unmodifiableSet(av);
    }

    private Map<String,Properties> getPropsByResourcePaths()
    {
	Map<String,Properties> out = new HashMap<String,Properties>();
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
	Map<String,Properties> pbrm = getPropsByResourcePaths();
	List<DelayedLogItem> pms  = getDelayedLogItems();

	return new BasicMultiPropertiesConfig( rps, pbrm, pms, allVetoed, allNotFound, allFaults );
    }

    @Override
    public String[] getPropertiesResourcePaths()
    { return (String[]) resourcePaths.clone(); }

    @Override
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

    @Override
    public Properties getPropertiesByPrefix(String pfx)
    {
	List<Map.Entry<Object,Object>> entries = new LinkedList<Map.Entry<Object,Object>>();
	for (int i = configs.length - 1; i >= 0; --i)
        {
            MultiPropertiesConfig config = configs[i];
            Properties check = config.getPropertiesByPrefix(pfx);
            if (check != null)
                entries.addAll( 0, check.entrySet() );
        }
        Properties out = new Properties();
        for (Iterator<Map.Entry<Object,Object>> ii = entries.iterator(); ii.hasNext(); )
            {
                Map.Entry<Object,Object> entry = ii.next();
                out.put( entry.getKey(), entry.getValue() );
            }
        return out;
    }

    @Override
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

    @Override
    public List<DelayedLogItem> getDelayedLogItems()
    { return parseMessages; }

    @Override
    public boolean wasRead(String resourcePath)
    { return allRead.contains(resourcePath); }

    @Override
    public boolean wasVetoed(String resourcePath)
    { return allVetoed.contains(resourcePath); }

    @Override
    public boolean wasNotFound(String resourcePath)
    { return allNotFound.contains(resourcePath); }

    @Override
    public boolean wasFault(String resourcePath)
    { return allFaults.contains(resourcePath); }

    @Override
    public Set<String> getAllRead()
    { return allRead; }

    @Override
    public Set<String> getAllVetoed()
    { return allVetoed; }

    @Override
    public Set<String> getAllNotFound()
    { return allNotFound; }

    @Override
    public Set<String> getAllFaults()
    { return allFaults; }
}

