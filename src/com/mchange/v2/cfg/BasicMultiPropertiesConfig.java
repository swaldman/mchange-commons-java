package com.mchange.v2.cfg;

import java.util.*;
import java.io.*;

import java.nio.file.NoSuchFileException;

import com.mchange.v3.hocon.HoconPropertiesConfigSource;

import static com.mchange.v2.cfg.DelayedLogItem.*;

final class BasicMultiPropertiesConfig extends MultiPropertiesConfig
{
    final static BasicMultiPropertiesConfig EMPTY = new BasicMultiPropertiesConfig();

    String[] rps;
    Map  propsByResourcePaths;
    Map  propsByPrefixes;

    List parseMessages;

    Properties propsByKey;

    Set<String> historyVetoed;
    Set<String> historyNotFound;
    Set<String> historyOtherFailure;

    static class VetoThrowing {
        static VetoThrowing INSTANCE = new VetoThrowing();
        private VetoThrowing() {}
    }

    // VetoThrowing implies MConfig.Kind.AsProvidedVetoable
    BasicMultiPropertiesConfig(VetoThrowing vetoThrowing, String[] resourcePaths, List delayedLogItems) throws ConfigVetoedException
    {
        boolean syserr = (delayedLogItems == null);
        List dlis = (syserr ? new ArrayList() : delayedLogItems);
        try
        {
            String[] safeResourcePaths = ConfigUtils.nullCheckPathArguments("resourcePaths", resourcePaths, dlis);
            firstInit( MConfig.Kind.AsProvidedVetoable, safeResourcePaths, dlis );
            finishInit( dlis );
        }
        finally
        {
            this.parseMessages = Collections.unmodifiableList( new ArrayList(dlis) );
            if ( syserr ) dumpToSysErr( dlis );
        }
    }

    // non-VetoThrowing implies MConfig.Kind.AsProvided or MConfig.Kind.Traditional
    BasicMultiPropertiesConfig(MConfig.Kind kind, String[] resourcePaths, List delayedLogItems)
    {
        boolean syserr = (delayedLogItems == null);
        List dlis = (syserr ? new ArrayList() : delayedLogItems);
        try
        {
            String[] safeResourcePaths = ConfigUtils.nullCheckPathArguments("resourcePaths", resourcePaths, dlis);
            if (kind != MConfig.Kind.AsProvided && kind != MConfig.Kind.Traditional)
                throw new IllegalArgumentException("Non-veto-throwing package private constructor must be of MConfig.Kind.AsProvided or MConfig.Kind.Traditional.");
            firstInitNotVetoThrowing( kind, safeResourcePaths, dlis );
            finishInit( dlis );
        }
        finally
        {
            this.parseMessages = Collections.unmodifiableList( new ArrayList(dlis) );
            if ( syserr ) dumpToSysErr( dlis );
        }
    }

    public BasicMultiPropertiesConfig(String[] resourcePaths)
    { this( MConfig.Kind.Traditional, resourcePaths, null ); } // mimics the traditional behavior of the library as closely as we now make available

    private BasicMultiPropertiesConfig(BasicMultiPropertiesConfig copyMe)
    {
        this.rps = (String[]) copyMe.rps.clone();
        this.propsByResourcePaths = Collections.unmodifiableMap(new HashMap(copyMe.propsByResourcePaths));
        this.propsByPrefixes = Collections.unmodifiableMap(new HashMap(copyMe.propsByPrefixes));
        this.parseMessages = Collections.unmodifiableList(new ArrayList(copyMe.parseMessages));
        this.propsByKey = new Properties();
        propsByKey.putAll( copyMe.propsByKey );

        this.historyVetoed = copyMe.historyVetoed;             // unmodifiable set
        this.historyNotFound = copyMe.historyNotFound;         // unmodifiable set
        this.historyOtherFailure = copyMe.historyOtherFailure; // unmodifiable set
    }

    BasicMultiPropertiesConfig(String[] rps, Map propsByResourcePaths, List parseMessages, Set<String> historyVetoed, Set<String> historyNotFound, Set<String> historyOtherFailures)
    {
	this.rps                  = (String[]) rps.clone();
	this.propsByResourcePaths = new HashMap(propsByResourcePaths);

	List dlis = new ArrayList();
	dlis.addAll( parseMessages );
	finishInit( dlis );

        this.historyVetoed       = Collections.unmodifiableSet(new HashSet(historyVetoed));
        this.historyNotFound     = Collections.unmodifiableSet(new HashSet(historyNotFound));
        this.historyOtherFailure = Collections.unmodifiableSet(new HashSet(historyOtherFailures));

	this.parseMessages = Collections.unmodifiableList(dlis);
    }

    BasicMultiPropertiesConfig(String[] rps, Map propsByResourcePaths, List parseMessages)
    { this( rps, propsByResourcePaths, parseMessages, Collections.<String>emptySet(), Collections.<String>emptySet(), Collections.<String>emptySet()); }
    
    public BasicMultiPropertiesConfig( String notionalResourcePath, Properties props )
    {
        this(
             (notionalResourcePath == null ? badNotionalResourcePath() : new String[] { notionalResourcePath }),
             resourcePathToPropertiesMap( notionalResourcePath, props ),
             Collections.emptyList()
        );
    }

    // EMPTY
    private BasicMultiPropertiesConfig()
    {
	this.rps                  = new String[0];
	this.propsByResourcePaths = Collections.emptyMap();
	this.propsByPrefixes      = Collections.emptyMap();
	this.parseMessages        = Collections.emptyList();
	this.propsByKey           = new Properties();

        this.historyVetoed =       Collections.emptySet();
        this.historyNotFound =      Collections.emptySet();
        this.historyOtherFailure = Collections.emptySet();
    }

    public BasicMultiPropertiesConfig withClassLoaderSafeParseMessages() // strips Throwables that might pin ClassLoaders
    {
        BasicMultiPropertiesConfig out = new BasicMultiPropertiesConfig(this);
        out.parseMessages = Collections.unmodifiableList(ConfigUtils.stripThrowables(this.parseMessages));
        return out;
    }

    private static Map resourcePathToPropertiesMap( String notionalResourcePath, Properties props )
    {
        if (notionalResourcePath == null) throw new IllegalArgumentException("notionalResourcePath should not be null.");
        if (props == null) throw new IllegalArgumentException("props should not be null.");
	Map out = new HashMap();
	out.put( notionalResourcePath, props );
	return out;
    }

    private static String[] badNotionalResourcePath()
    { throw new IllegalArgumentException("notionalResourcePath must not be null"); }

    private void firstInitNotVetoThrowing( MConfig.Kind kind, String[] resourcePaths, List delayedLogItems )
    {
        try { firstInit( kind, resourcePaths, delayedLogItems ); }
        catch (ConfigVetoedException cve)
        { throw new RuntimeException("Internal inconsistency! All ConfigVetoedExceptions should have been handled by this point: " + cve, cve); }
    }

    private void logCveForAsProvided(ConfigVetoedException cve, List delayedLogItems)
    {
        String identifier = cve.getIdentifier();
        String identifierPart = identifier == null ? "" : " The resource it was trying to read was '" + identifier + "'. Config provided by identifier will be ignored.";
        String longAssMessage =
            "A PropertiesConfigSource tried to veto config in a BasicMultiPropertiesConfig that should have excluded vetoable config! " +
            "This is either a bug in the com.mchange.v2.cfg library, or a bug in the PropertiesConfigSource, which should not throw " +
            "a ConfigVetoedException unless it implements the VetoableConfig interface, which should have prevented its participation " +
            "in this PropertiesConfigSource. The PropertiesConfigSource that threw the unexpected Exception was " + cve.getSource() + "." +
            identifierPart;
        delayedLogItems.add( new DelayedLogItem( Level.WARNING, longAssMessage, cve ) );
    }

    private void logCveForAsProvidedVetoable(boolean willBeFirst, ConfigVetoedException cve, List delayedLogItems)
    {
        String identifier = cve.getIdentifier();
        String identifierPart = identifier == null ? "." : " while handling identifier '" + identifier + "'.";
        String msg = "A PropertiesConfigSource (" + cve.getSource() + ") has vetoed config in a BasicMultiPropertiesConfig" + identifierPart + (willBeFirst ? " An Exception will be thrown." : "");
        delayedLogItems.add( new DelayedLogItem( Level.WARNING, msg, cve ) );
    }

    private void logCveForTraditional(ConfigVetoedException cve, List delayedLogItems)
    {
        String identifier = cve.getIdentifier();
        String identifierPart = identifier == null ? "." : " while handling identifier '" + identifier + "'.";
        String msg = "A PropertiesConfigSource (" + cve.getSource() + ") has vetoed config in a BasicMultiPropertiesConfig" + identifierPart + " Any associated configuration has been ignored!";
        delayedLogItems.add( new DelayedLogItem( Level.WARNING, msg, cve ) );
    }

    private void firstInit( MConfig.Kind kind, String[] resourcePaths, List delayedLogItems ) throws ConfigVetoedException
    {
        Map  pbrp = new HashMap();
        List goodPaths = new ArrayList();

        Set<String> _historyVetoed       = new HashSet<>();
        Set<String> _historyNotFound      = new HashSet<>();
        Set<String> _historyOtherFailure = new HashSet<>();

        List<ConfigVetoedException> cves = new ArrayList<>();

        for( int i = 0, len = resourcePaths.length; i < len; ++i )
        {
            String rp = resourcePaths[i];

            try
            {
                PropertiesConfigSource cs = ConfigUtils.propertiesConfigSourceForIdentifier( rp, delayedLogItems );
                if (cs == null)
                    throw new FileNotFoundException( "'" + rp + "' is not valid or could not be found. Skipping." );
                else
                {
                    PropertiesConfigSource.Parse parse = cs.propertiesFromSource( rp );
                    pbrp.put( rp, parse.getProperties() );
                    goodPaths.add( rp );
                    delayedLogItems.addAll( parse.getDelayedLogItems() );
                }
            }
            catch (ConfigVetoedException cve)
            {
                _historyVetoed.add(rp);
                cves.add(cve);
                delayedLogItems.addAll( cve.getDelayedLogItems() );
            }
            catch (OwnLogCarryingMissingFileException lcmfe)
            {
                _historyNotFound.add(rp);
                delayedLogItems.addAll( lcmfe.getDelayedLogItems() ); // its own report, in place of the generic one
            } 
            catch (ConfigParseException cpe) // catch-all
            {
                _historyOtherFailure.add(rp);
                delayedLogItems.addAll( cpe.getDelayedLogItems() );
            }
            catch ( NoSuchFileException nsfe )
            {
                _historyNotFound.add(rp);
                delayedLogItems.add( MConfig.skippingFileNotFoundDelayedItem(rp,nsfe) );
            }
            catch ( FileNotFoundException fnfe )
            {
                _historyNotFound.add(rp);
                delayedLogItems.add( MConfig.skippingFileNotFoundDelayedItem(rp,fnfe) );
            }
            catch ( Exception e )
            {
                _historyOtherFailure.add(rp);
                delayedLogItems.add( new DelayedLogItem( Level.WARNING, String.format("An Exception occurred while trying to read configuration data at resource identifier '%s'.", rp), e) );
            }
        }

        this.rps = (String[]) goodPaths.toArray( new String[ goodPaths.size() ] );
        this.propsByResourcePaths = Collections.unmodifiableMap( pbrp );

        this.historyVetoed       = Collections.unmodifiableSet(_historyVetoed);
        this.historyNotFound     = Collections.unmodifiableSet(_historyNotFound);
        this.historyOtherFailure = Collections.unmodifiableSet(_historyOtherFailure);

        if (cves.size() != 0)
        {
            if (kind == MConfig.Kind.AsProvided)
                for (ConfigVetoedException cve : cves)
                    logCveForAsProvided(cve, delayedLogItems);
            else if (kind == MConfig.Kind.AsProvidedVetoable)
            {
                ConfigVetoedException first = null;
                for (ConfigVetoedException cve : cves)
                {
                    logCveForAsProvidedVetoable(first == null, cve, delayedLogItems);
                    if (first == null) first = cve;
                }
                throw first;
            }
            else if (kind == MConfig.Kind.Traditional)
                for (ConfigVetoedException cve : cves)
                    logCveForTraditional(cve, delayedLogItems);
        }
    }

    /**
     *  rps and propsByResourcePaths should be set before finishInit()
     */
    private void finishInit( List delayedLogItems )
    {
	this.propsByPrefixes = Collections.unmodifiableMap( extractPrefixMapFromRsrcPathMap(rps, propsByResourcePaths, delayedLogItems ) );
	this.propsByKey = extractPropsByKey(rps, propsByResourcePaths, delayedLogItems );
    }

    @Override
    public List getDelayedLogItems()
    { return parseMessages; }

    @Override
    public boolean wasRead(String resourcePath)
    { return propsByResourcePaths.keySet().contains(resourcePath); }

    @Override
    public boolean wasVetoed(String resourcePath)
    { return historyVetoed.contains(resourcePath); }

    @Override
    public boolean wasNotFound(String resourcePath)
    { return historyNotFound.contains(resourcePath); }

    @Override
    public boolean wasFault(String resourcePath)
    { return historyOtherFailure.contains(resourcePath); }

    @Override
    public Set getAllRead()
    { return Collections.unmodifiableSet( propsByResourcePaths.keySet() ); }

    @Override
    public Set getAllVetoed()
    { return historyVetoed; }

    @Override
    public Set getAllNotFound()
    { return historyNotFound; }

    @Override
    public Set getAllFaults()
    { return historyOtherFailure; }

    private static void dumpToSysErr( List delayedLogMessages )
    {
	for (Object o : delayedLogMessages)
	    System.err.println( o );
    }

    private static String extractPrefix( String s )
    {
	int lastdot = s.lastIndexOf('.');
	if ( lastdot < 0 )
	{
	    if ( "".equals( s ) )
		return null;
	    else
		return "";
        }
	else
	    return s.substring(0, lastdot);
    }

    private static Properties findProps(String rp, Map pbrp)
    {
	//System.err.println("findProps( " + rp + ", ... )");
	Properties p;
	
	// MOVED THIS LOGIC INTO CONSTRUCTOR ABOVE, TO TREAT SYSTEM PROPS UNIFORMLY
	// WITH THE REST, AND TO AVOID UNINTENTIONAL ATTEMPTS TO READ RESOURCE "/"
	// AS STREAM -- swaldman, 2006-01-19
	
// 	if ( "/".equals( rp ) )
// 	    {
// 		try { p = System.getProperties(); }
// 		catch ( SecurityException e )
// 		    {
// 			System.err.println(BasicMultiPropertiesConfig.class.getName() +
// 					   " Read of system Properties blocked -- ignoring any configuration via System properties, and using Empty Properties! " +
// 					   "(But any configuration via a resource properties files is still okay!)"); 
// 			p = new Properties(); 
// 		    }
// 	    }
// 	else
	p = (Properties) pbrp.get( rp );
	
// 	System.err.println( p );

	return p;
    }

    private static Properties extractPropsByKey( String[] resourcePaths, Map pbrp, List delayedLogItems )
    {
	Properties out = new Properties();
	for (int i = 0, len = resourcePaths.length; i < len; ++i)
	    {
		String rp = resourcePaths[i];
		Properties p = findProps( rp, pbrp );
		if (p == null)
		    {
			delayedLogItems.add( new DelayedLogItem( Level.WARNING, BasicMultiPropertiesConfig.class.getName() + ".extractPropsByKey(): Could not find loaded properties for resource path: " + rp) );
			//System.err.println("Could not find loaded properties for resource path: " + rp);
			continue;
		    }
		for (Iterator ii = p.keySet().iterator(); ii.hasNext(); )
		    {
			Object kObj = ii.next();
			if (!(kObj instanceof String))
			    {
				String message = 
				    BasicMultiPropertiesConfig.class.getName() + ": " +
				    "Properties object found at resource path " +
				    ("/".equals(rp) ? "[system properties]" : "'" + rp + "'") +
				    "' contains a key that is not a String: " +
				    kObj +
				    "; Skipping...";

				/*
				// note that we can not use the MLog library here, because initialization
				// of that library depends on this function.
				System.err.println( BasicMultiPropertiesConfig.class.getName() + ": " +
						    "Properties object found at resource path " +
						    ("/".equals(rp) ? "[system properties]" : "'" + rp + "'") +
						    "' contains a key that is not a String: " +
						    kObj);
				System.err.println("Skipping...");
				*/
				delayedLogItems.add( new DelayedLogItem( Level.WARNING, message) );
				continue;
			    }
			Object vObj = p.get( kObj );
			if (vObj != null && !(vObj instanceof String))
			    {
				String message =
				    BasicMultiPropertiesConfig.class.getName() + ": " +
				    "Properties object found at resource path " +
				    ("/".equals(rp) ? "[system properties]" : "'" + rp + "'") +
				    " contains a value that is not a String: " +
				    vObj +
				    "; Skipping...";

				/*
				// note that we can not use the MLog library here, because initialization
				// of that library depends on this function.
				System.err.println( BasicMultiPropertiesConfig.class.getName() + ": " +
						    "Properties object found at resource path " +
						    ("/".equals(rp) ? "[system properties]" : "'" + rp + "'") +
						    " contains a value that is not a String: " +
						    vObj);
				System.err.println("Skipping...");
				*/
				delayedLogItems.add( new DelayedLogItem( Level.WARNING, message) );
				continue;
			    }

			String key = (String) kObj;
			String val = (String) vObj;
			out.put( key, val );
		    }
	    }
	return out;
    }

    private static Map extractPrefixMapFromRsrcPathMap(String[] resourcePaths, Map pbrp, List delayedLogItems )
    {
	Map out = new HashMap();
	//for( Iterator ii = pbrp.values().iterator(); ii.hasNext(); )
	for (int i = 0, len = resourcePaths.length; i < len; ++i)
	    {
		String rp = resourcePaths[i];
		Properties p = findProps( rp, pbrp );
		if (p == null)
		    {
			String message = BasicMultiPropertiesConfig.class.getName() + ".extractPrefixMapFromRsrcPathMap(): Could not find loaded properties for resource path: " + rp;
			//System.err.println(BasicMultiPropertiesConfig.class.getName() + " -- Could not find loaded properties for resource path: " + rp);
			delayedLogItems.add( new DelayedLogItem( Level.WARNING, message) );
			continue;
		    }
		for (Iterator jj = p.keySet().iterator(); jj.hasNext(); )
		    {
			Object kObj = jj.next();
			if (! (kObj instanceof String))
			    {
				String message =
				    BasicMultiPropertiesConfig.class.getName() + ": " +
				    "Properties object found at resource path " +
				    ("/".equals(rp) ? "[system properties]" : "'" + rp + "'") +
				    "' contains a key that is not a String: " +
				    kObj +
				    "; Skipping...";

				/*
				// note that we can not use the MLog library here, because initialization
				// of that library depends on this function.
				System.err.println( BasicMultiPropertiesConfig.class.getName() + ": " +
						    "Properties object found at resource path " +
						    ("/".equals(rp) ? "[system properties]" : "'" + rp + "'") +
						    "' contains a key that is not a String: " +
						    kObj);
				System.err.println("Skipping...");
				*/

				delayedLogItems.add( new DelayedLogItem( Level.WARNING, message) );
				continue;
			    }

			String key = (String) kObj;
			String prefix = extractPrefix( key );
			while (prefix != null)
			    {
				Properties byPfx = (Properties) out.get( prefix );
				if (byPfx == null)
				    {
					byPfx = new Properties();
					out.put( prefix, byPfx );
				    }
				byPfx.put( key, p.get( key ) );

				prefix=extractPrefix( prefix );
			    }
		    }
	    }
	return out;
    }

    @Override
    public String[] getPropertiesResourcePaths()
    { return (String[]) rps.clone(); }

    @Override
    public Properties getPropertiesByResourcePath(String path)
    { 
	Properties out = ((Properties) propsByResourcePaths.get( path )); 
	return (out == null ? new Properties() : out);
    }

    @Override
    public Properties getPropertiesByPrefix(String pfx)
    {
	Properties out = ((Properties) propsByPrefixes.get( pfx ));
	return (out == null ? new Properties() : out);
    }

    @Override
    public String getProperty( String key )
    { return propsByKey.getProperty( key ); }

//    public Properties getProperties()
//    { return (Properties) propsByKey.clone(); }

    //TODO: Make this much prettier
    public String dump()
    { return String.format("[ propertiesByResourcePaths -> %s, propertiesByPrefixes -> %s ]", propsByResourcePaths, propsByPrefixes); }

    @Override
    public String toString()
    { return super.toString() + " " + this.dump(); }
}
