package com.mchange.v2.cfg;

import java.util.*;
import java.io.*;

import java.nio.file.NoSuchFileException;

import com.mchange.v3.hocon.HoconPropertiesConfigSource;

import static com.mchange.v2.cfg.DelayedLogItem.*;

final class BasicMultiPropertiesConfig extends MultiPropertiesConfig
{
    private final static String HOCON_CFG_CNAME = "com.typesafe.config.Config";
    private final static int    HOCON_PFX_LEN   = 6; // includes colon, hocon:

    final static BasicMultiPropertiesConfig EMPTY = new BasicMultiPropertiesConfig();

    String[] rps;
    Map  propsByResourcePaths;
    Map  propsByPrefixes;

    List parseMessages;

    Properties propsByKey;

    static class VetoThrowing {
        static VetoThrowing INSTANCE = new VetoThrowing();
        private VetoThrowing() {}
    }

    public BasicMultiPropertiesConfig(String[] resourcePaths)
    { this( MConfig.Kind.Traditional, resourcePaths, null ); } // mimics the traditional behavior of the library as closely as we now make available

    // VetoThrowing implies MConfig.Kind.AsProvidedVetoable
    BasicMultiPropertiesConfig(VetoThrowing vetoThrowing, String[] resourcePaths, List delayedLogItems) throws ConfigVetoedException
    {
	firstInit( MConfig.Kind.AsProvidedVetoable, resourcePaths, delayedLogItems );
	finishInit( delayedLogItems );
    }

    // non-VetoThrowing implies MConfig.Kind.AsProvided or MConfig.Kind.Traditional
    BasicMultiPropertiesConfig(MConfig.Kind kind, String[] resourcePaths, List delayedLogItems)
    {
        if (kind != MConfig.Kind.AsProvided && kind != MConfig.Kind.Traditional)
            throw new IllegalArgumentException("Non-veto-throwing package private constructor must be of MConfig.Kind.AsProvided or MConfig.Kind.Traditional.");
	firstInitNotVetoThrowing( kind, resourcePaths, delayedLogItems );
	finishInit( delayedLogItems );
    }

    public BasicMultiPropertiesConfig( String notionalResourcePath, Properties props )
    { this( new String[] { notionalResourcePath }, resourcePathToPropertiesMap( notionalResourcePath, props ), Collections.emptyList() ); }

    private static Map resourcePathToPropertiesMap( String notionalResourcePath, Properties props )
    {
	Map out = new HashMap();
	out.put( notionalResourcePath, props );
	return out;
    }

    BasicMultiPropertiesConfig(String[] rps, Map propsByResourcePaths, List parseMessages)
    {
	this.rps                  = rps;
	this.propsByResourcePaths = propsByResourcePaths;

	List dlis = new ArrayList();
	dlis.addAll( parseMessages );
	finishInit( dlis );

	this.parseMessages = dlis;
    }

    // EMPTY
    private BasicMultiPropertiesConfig()
    {
	// NOTE: every assignment below must target a field. These were once local
	// declarations that merely shadowed the fields, leaving the EMPTY singleton
	// with null propsByPrefixes, parseMessages, and propsByKey -- so getProperty,
	// getPropertiesByPrefix, and getDelayedLogItems all threw NullPointerException.
	this.rps                  = new String[0];
	this.propsByResourcePaths = Collections.emptyMap();
	this.propsByPrefixes      = Collections.emptyMap();
	this.parseMessages        = Collections.emptyList();
	this.propsByKey           = new Properties();
    }

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

    private void logCveForAsProvidedVetoable(ConfigVetoedException cve, List delayedLogItems)
    {
        String identifier = cve.getIdentifier();
        String identifierPart = identifier == null ? "." : " while handling identifier '" + identifier + "'.";
        String msg = "A PropertiesConfigSource (" + cve.getSource() + ") has vetoed config in a BasicMultiPropertiesConfig" + identifierPart + " An Exception will be thrown.";
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
	boolean syserr = false;
        if (delayedLogItems == null)
        {
            delayedLogItems = new ArrayList();
            syserr = true;
        }

        try
        {

            Map  pbrp = new HashMap();
            List goodPaths = new ArrayList();

            List<ConfigVetoedException> cves = new ArrayList<>();

            for( int i = 0, len = resourcePaths.length; i < len; ++i )
            {
                String rp = resourcePaths[i];

                try
                {
                    PropertiesConfigSource cs = ConfigUtils.propertiesConfigSourceForIdentifier( rp, delayedLogItems );
                    if (cs == null)
                        throw new FileNotFoundException( "'" + rp + "' could not be found. Skipping." );
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
                    cves.add(cve);
                    delayedLogItems.addAll( cve.getDelayedLogItems() );
                }
                catch (OwnLogCarryingMissingFileException lcmfe)
                { delayedLogItems.addAll( lcmfe.getDelayedLogItems() ); } // its own report, in place of the generic one
                catch (ConfigParseException cpe) // catch-all
                { delayedLogItems.addAll( cpe.getDelayedLogItems() ); }
                catch ( NoSuchFileException nsfe )
                { delayedLogItems.add( MConfig.skippingFileNotFoundDelayedItem(rp,nsfe) ); }
                catch ( FileNotFoundException fnfe )
                { delayedLogItems.add( MConfig.skippingFileNotFoundDelayedItem(rp,fnfe) ); }
                catch ( Exception e )
                { delayedLogItems.add( new DelayedLogItem( Level.WARNING, String.format("An Exception occurred while trying to read configuration data at resource identifier '%s'.", rp), e) ); }
            }

            this.rps = (String[]) goodPaths.toArray( new String[ goodPaths.size() ] );
            this.propsByResourcePaths = Collections.unmodifiableMap( pbrp );

            if (cves.size() != 0)
            {
                if (kind == MConfig.Kind.AsProvided)
                    for (ConfigVetoedException cve : cves)
                        logCveForAsProvided(cve, delayedLogItems);
                else if (kind == MConfig.Kind.AsProvidedVetoable)
                {
                    ConfigVetoedException last = null;
                    for (ConfigVetoedException cve : cves)
                    {
                        last = cve;
                        logCveForAsProvidedVetoable(cve, delayedLogItems);
                    }
                    throw last;
                }
                else if (kind == MConfig.Kind.Traditional)
                    for (ConfigVetoedException cve : cves)
                        logCveForTraditional(cve, delayedLogItems);
            }
        }
        finally
        {
            this.parseMessages = Collections.unmodifiableList( delayedLogItems );
            if ( syserr )
                dumpToSysErr( delayedLogItems );
        }
    }

    /**
     *  rps, propsByResourcePaths, and parseMessages should be set before finishInit()
     */
    private void finishInit( List delayedLogItems )
    {
	boolean syserr = false;
	if (delayedLogItems == null)
	    {
		delayedLogItems = new ArrayList();
		syserr = true;
	    }

	this.propsByPrefixes = Collections.unmodifiableMap( extractPrefixMapFromRsrcPathMap(rps, propsByResourcePaths, delayedLogItems ) );
	this.propsByKey = extractPropsByKey(rps, propsByResourcePaths, delayedLogItems );

	if ( syserr )
	    dumpToSysErr( delayedLogItems );
    }

    public List getDelayedLogItems()
    { return parseMessages; }

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

    public String[] getPropertiesResourcePaths()
    { return (String[]) rps.clone(); }

    public Properties getPropertiesByResourcePath(String path)
    { 
	Properties out = ((Properties) propsByResourcePaths.get( path )); 
	return (out == null ? new Properties() : out);
    }

    public Properties getPropertiesByPrefix(String pfx)
    {
	Properties out = ((Properties) propsByPrefixes.get( pfx ));
	return (out == null ? new Properties() : out);
    }

    public String getProperty( String key )
    { return propsByKey.getProperty( key ); }

//    public Properties getProperties()
//    { return (Properties) propsByKey.clone(); }

    //TODO: Make this much prettier
    public String dump()
    { return String.format("[ propertiesByResourcePaths -> %s, propertiesByPrefixes -> %s ]", propsByResourcePaths, propsByPrefixes); }

    public String toString()
    { return super.toString() + " " + this.dump(); }
}
