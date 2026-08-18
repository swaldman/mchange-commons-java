package com.mchange.v2.cfg;

import java.util.*;
import java.io.*;

import static com.mchange.v2.cfg.DelayedLogItem.*;

// external clients should go through the MConfig facade.
final class ConfigUtils
{
    // we retain "/com/mchange/v2/cfg/vmConfigResourcePaths.txt" for backwards compatibility,
    // but we are trying to get rid of the ill-defined concept of "vmConfig"
    private final static String[] DFLT_RSRC_PATHFILES       = new String[] {"/com/mchange/v2/cfg/vmConfigResourcePaths.txt", "/com/mchange/v2/cfg/defaultConfigResourcePaths.txt", "/mchange-config-resource-paths.txt"};

    // later paths override earlier paths
    private final static String[] HARDCODED_DFLT_RSRC_PATHS = new String[]
	{
	    "/mchange-commons.properties",
	    "hocon:/reference,/application,/",
	    "/"
	};

    final static String[] NO_PATHS = new String[0];

    private final static String HOCON_PFX = "hocon:";

    //MT: protected by class' lock
    static MultiPropertiesConfig canonicalDefaultConfig = null;

    //public static MultiPropertiesConfig read(String[] resourcePath, MLogger logger)
    //{ return new BasicMultiPropertiesConfig( resourcePath, logger ); }

    static MultiPropertiesConfig read(String[] resourcePath, List delayedLogItems)
    { return new BasicMultiPropertiesConfig( resourcePath, delayedLogItems ); }

    public static MultiPropertiesConfig read(String[] resourcePath)
    { return new BasicMultiPropertiesConfig( resourcePath ); }

    /**
     *  Later entries in the configs array override earlier entries.
     */
    public static MultiPropertiesConfig combine( MultiPropertiesConfig[] configs )
    { return new CombinedMultiPropertiesConfig( configs ).toBasic(); }

    static MultiPropertiesConfig readUncachedClassloaderResourceConfig(boolean withDefaults, String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    {
        String[] paths = condenseResources( withDefaults, defaultResources, preemptingResources, delayedLogItemsOut );
        return read( paths, delayedLogItemsOut );
    }

    static String[] condenseResources(boolean withDefaults, String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    {
	defaultResources = ( defaultResources == null ? NO_PATHS : defaultResources );
	preemptingResources = ( preemptingResources == null ? NO_PATHS : preemptingResources );
	List pathsList;
        List raw;
        if (withDefaults)
            raw = configuredOrHardcodedDefaultClassloaderResourcePathsCondensed( defaultResources, preemptingResources, delayedLogItemsOut );
        else
            raw = asProvidedClassloaderResourcePathsCondensed( defaultResources, preemptingResources, delayedLogItemsOut );
        pathsList = ensureHoconInterresolvability( raw );

	if ( delayedLogItemsOut != null )
	    delayedLogItemsOut.add( new DelayedLogItem(Level.FINER, "Reading classloader-resource-based config for path list " + stringFromPathsList( pathsList ) ) );

	return (String[]) pathsList.toArray(new String[pathsList.size()]);
    }

    private static List configuredOrHardcodedDefaultClassloaderResourcePathsCondensed(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    { return condensePaths( new String[][]{ defaultResources, configuredOrHardcodedDefaultClassloaderResourcePaths( delayedLogItemsOut ), preemptingResources } ); }

    private static List asProvidedClassloaderResourcePathsCondensed(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    { return condensePaths( new String[][]{ defaultResources, preemptingResources } ); }

    static String stringFromPathsList( List pathsList )
    {
	StringBuffer sb = new StringBuffer(2048);
	for ( int i = 0, len = pathsList.size(); i < len; ++i)
	    {
		if ( i != 0 ) sb.append(", ");
		sb.append( pathsList.get(i) );
	    }
	return sb.toString();
    }

    private static List condensePaths(String[][] pathLists)
    {
	// we do this in reverse, so that the "first" time
	// we encounter a path becomes the last in the resultant
	// list. that is, we want redundantly specified paths
	// to have their maximum specified preference

	Set pathSet = new HashSet();
	List reverseMe = new ArrayList();
	for ( int i = pathLists.length; --i >= 0; )
	    for( int j = pathLists[i].length; --j >= 0; )
	    {
		String path = pathLists[i][j];
		if (! pathSet.contains( path ) )
		{
		    pathSet.add( path );
		    reverseMe.add( path );
		}
	    }
	 Collections.reverse( reverseMe );
	 return reverseMe;
    }

    private static List readResourcePathsFromResourcePathsTextFile( String resourcePathsTextFileResourcePath,  List delayedLogItemsOut )
    {
	List rps = new ArrayList();

	BufferedReader br = null;
	try
	    {
		InputStream is = ConfigUtils.class.getResourceAsStream( resourcePathsTextFileResourcePath );
		if ( is != null )
		    {
			br = new BufferedReader( new InputStreamReader( is, "8859_1" ) );
			String rp;
			while ((rp = br.readLine()) != null)
			    {
				rp = rp.trim();
				if ("".equals( rp ) || rp.startsWith("#"))
				    continue;

				rps.add( rp );
			    }

			if ( delayedLogItemsOut != null )
			    delayedLogItemsOut.add( new DelayedLogItem( Level.FINEST, String.format( "Added paths from resource path text file at '%s'", resourcePathsTextFileResourcePath ) ) );
		    }
		else if ( delayedLogItemsOut != null )
		    delayedLogItemsOut.add( new DelayedLogItem( Level.FINEST, String.format( "Could not find resource path text file for path '%s'. Skipping.", resourcePathsTextFileResourcePath ) ) );

	    }
	catch (IOException e)
	    { e.printStackTrace(); }
	finally
	    {
		try { if ( br != null ) br.close(); }
		catch (IOException e) { e.printStackTrace(); }
	    }

	return rps;
    }

    private static List readResourcePathsFromResourcePathsTextFiles( String[] resourcePathsTextFileResourcePaths, List delayedLogItemsOut )
    {
	List out = new ArrayList();
	for ( int i = 0, len = resourcePathsTextFileResourcePaths.length; i < len; ++i )
	    out.addAll( readResourcePathsFromResourcePathsTextFile(  resourcePathsTextFileResourcePaths[i], delayedLogItemsOut ) );
	return out;
    }

    private static String[] configuredOrHardcodedDefaultClassloaderResourcePaths( List delayedLogItemsOut )
    {
	List paths = configuredOrHardcodedDefaultClassloaderResourcePathList(  delayedLogItemsOut );
	return (String[]) paths.toArray( new String[ paths.size() ] );
    }

    private static List configuredOrHardcodedDefaultClassloaderResourcePathList( List delayedLogItemsOut )
    {
	List pathsFromFiles = readResourcePathsFromResourcePathsTextFiles( DFLT_RSRC_PATHFILES, delayedLogItemsOut );
	List rps;
	if ( pathsFromFiles.size() > 0 )
	    rps = pathsFromFiles;
	else
	    rps = Arrays.asList( HARDCODED_DFLT_RSRC_PATHS );
	return rps;
    }
    public synchronized static MultiPropertiesConfig readCanonicalDefaultConfig()
    { return readVmConfig( (List) null ); }

    /*
    public synchronized static MultiPropertiesConfig readVmConfig( MLogger logger )
    {
	List items = new ArrayList();
	MultiPropertiesConfig out = readVmConfig( items );
	items.addAll( out.getDelayedLogItems() );
	for (Iterator ii = items.iterator(); ii.hasNext(); )
	{
	    DelayedLogItem item = (DelayedLogItem) ii.next();
	    logger.log( item.getLevel(), item.getText(), item.getException() );
	}
	return out;
    }
    */

    public synchronized static MultiPropertiesConfig readCanonicalDefaultConfig( List delayedLogItemsOut )
    {
	if ( canonicalDefaultConfig == null )
	    {
		List rps = configuredOrHardcodedDefaultClassloaderResourcePathList( delayedLogItemsOut );
		canonicalDefaultConfig = new BasicMultiPropertiesConfig( (String[]) rps.toArray( new String[ rps.size() ] ) );
	    }
	return canonicalDefaultConfig;
    }

    public static synchronized boolean foundCanonicalDefaultConfig()
    { return canonicalDefaultConfig != null; }

    public static void dumpByPrefix( MultiPropertiesConfig mpc, String pfx )
    {
	Properties props = mpc.getPropertiesByPrefix(pfx);
	Map m = new TreeMap();
	m.putAll( props );
	for ( Iterator ii = m.entrySet().iterator(); ii.hasNext(); )
	{
	    Map.Entry entry = (Map.Entry) ii.next();
	    System.err.println( entry.getKey() + " --> " + entry.getValue() );
	}
    }

    /* Multimap insert: map.get(key) is a Set that is created on first use. */
    private static void putToSet(Map<String,Set<String>> map, String key, String value ) {
	Set<String> set = map.get( key );
	if ( set == null ) {
	    set = new HashSet<String>();
	    map.put( key, set );
	}
	set.add( value );
    }

    /* Reassembles an element list back into a "hocon:a,b,c" identifier. Inverse of
     * hoconPathElements, and the form BasicMultiPropertiesConfig expects to receive. */
    private static String makeHoconPathFromElements( List<String> newElementsList ) {
	StringBuilder sb = new StringBuilder();
	sb.append(HOCON_PFX);
	boolean first = true;
	for( String element : newElementsList ) {
	    if ( first ) first = false;
	    else sb.append(",");
	    sb.append( element );
	}
	return sb.toString();
    }

    /*
     * Puts an element into a canonical form so that overlap detection can compare elements as
     * strings: "application" and "/application" name the same resource and must index alike.
     * Elements containing ':' are URLs and are left exactly as written.
     */
    private static String normalizeHoconPathElement( String element ) {
	if ( element.length() == 0 ) // guarded so charAt(0) below cannot throw; callers drop empties anyway
	    return element;
	return ( element.indexOf(":") < 0 && element.charAt(0) != '/' ) ? ('/' + element) : element;
    }

    /*
     * Splits the element list of a HOCON path, dropping empty elements.
     *
     * An empty element arises from a leading or doubled comma ("hocon:,/reference",
     * "hocon:/a,,/b"). Left in place it used to reach normalizeHoconPathElement and
     * throw StringIndexOutOfBoundsException straight out of the MConfig facade,
     * bypassing the DelayedLogItem machinery that is supposed to absorb bad config.
     */
    private static List<String> hoconPathElements( String path ) {
	String[] raw = path.substring( HOCON_PFX.length() ).split("\\s*,\\s*");
	List<String> out = new ArrayList<String>( raw.length );
	for ( String element : raw ) {
	    String trimmed = element.trim();
	    if ( trimmed.length() > 0 )
		out.add( normalizeHoconPathElement( trimmed ) );
	}
	return out;
    }

    /*
     * Null-safe union helper for pass 2 of ensureHoconInterresolvability. A lookup can miss
     * whenever pass 2 probes a suffix expansion that pass 1 had no reason to register.
     */
    private static void addHoconPathsFor( Map<String,Set<String>> elementToHoconPaths, Set<String> accum, String element ) {
	Set<String> found = elementToHoconPaths.get( element );
	if ( found != null ) accum.addAll( found );
    }


    /*
     * Well, this is a pain.
     *
     * BACKGROUND. A single HOCON path in our resource-path list, say
     *
     *     hocon:/reference,/application,/
     *
     * is not three independent config sources. HoconPropertiesConfigSource merges all of a
     * path's elements into ONE typesafe Config and then calls resolve() on the merged whole,
     * which is where ${...} substitutions get looked up. Resolution is therefore all-or-nothing
     * PER HOCON PATH: if any substitution anywhere in the path has no definition anywhere else
     * in that same path, resolve() throws and the entire path contributes nothing -- not just
     * the file with the dangling reference, but every element of that path.
     *
     * THE PROBLEM. We let several independently-authored HOCON paths coexist in one VM. Two
     * different libraries each register their own, and they routinely name some of the same
     * resources, because 'reference' and 'application' are the typesafe-config conventions and
     * everyone reaches for them. Now:
     *
     *     library A registers   hocon:/reference,/application,/
     *     library B registers   hocon:/b-reference,/application,/
     *
     * The user writes one application.conf, and in it writes ${some.key} where some.key is
     * defined in A's reference.conf. Perfectly reasonable: in A's path it resolves.
     *
     * But B's path also reads that same application.conf, and B's path has no definition of
     * some.key. So B's resolve() throws, and B silently loses ALL of its configuration --
     * including b-reference.conf, which never had anything to do with the substitution. A user
     * writing an entirely legitimate config for A has broken B, invisibly.
     *
     * THE FIX. Detect HOCON paths that OVERLAP -- that share at least one resource element --
     * and let overlapping paths fall back to one another. Sharing an element is our evidence
     * that two paths read some of the same user-authored files, and so are liable to see each
     * other's substitutions. For each such path we rewrite its element list as:
     *
     *     [ elements of every other overlapping path, in VM-specified order ] ++ [ its own elements ]
     *
     * Own elements go LAST because HOCON merge is last-wins, so a path's explicit specification
     * still overrides everything it borrowed; the borrowed material sits behind it, available to
     * satisfy substitutions but unable to overwrite anything the path actually declared. And the
     * borrowed paths keep their relative VM ordering, so "a later path in the list wins" still
     * holds among the borrowings.
     *
     * Concretely, reduced to the smallest pair that shows it -- common.conf contains
     * greeting = "hello "${who}, app1.conf defines who, app2.conf does not:
     *
     *     path 1: hocon:/common,/app1     ->   hocon:/common,/app2,/common,/app1
     *     path 2: hocon:/common,/app2     ->   hocon:/common,/app1,/common,/app2
     *
     * Before the rewrite, path 2 could not resolve ${who} and lost app2's settings along with
     * it. After, both resolve, and each still ends with its own elements, so path 1 still sees
     * app1 winning and path 2 still sees app2 winning. (This is exactly the fixture in
     * HoconConfigJUnitTestCase.testOverlappingHoconPathsResolveEachOthersSubstitutions.)
     *
     * THE TRADE-OFF, stated honestly: borrowing elements can introduce keys a path never asked
     * for, behind its own definitions. That is the price of not having unrelated libraries
     * silently zero each other out, and it only applies between paths that already share files.
     */
    private static List<String> ensureHoconInterresolvability( List<String> paths ) {
	// path -> its normalized element list, so pass 2 need not re-split
	Map<String,List<String>> hoconPathToElementsList = new HashMap<String,List<String>>();

	// element -> every HOCON path that reads it. This is the overlap index: two paths
	// overlap exactly when some element maps to a set containing both of them.
	Map<String,Set<String>>  elementToHoconPaths     = new HashMap<String,Set<String>>();

	List<String> out = new ArrayList<String>();

	// pass 1 -- build the indices
	for ( String path : paths ) {
	    if (BasicMultiPropertiesConfig.isHoconPath( path )) {
		List<String> elements = hoconPathElements( path );
		hoconPathToElementsList.put( path, elements );
		for (String element : elements ) {
		    putToSet(elementToHoconPaths, element, path );

		    // An element with no '.' is a suffix-less name, which typesafe-config reads
		    // as ALL of name.conf, name.json and name.properties. So "/application" and
		    // "/application.conf" genuinely read the same file and genuinely overlap,
		    // even though the two strings differ. Register the expansions under this
		    // path so a suffixed element elsewhere can find us. (Pass 2 expands on
		    // lookup too -- overlap is symmetric, and both passes must agree.)
		    if ( element.indexOf('.') < 0 && !"/".equals( element ) ) {
			putToSet( elementToHoconPaths, element + ".conf", path );
			putToSet( elementToHoconPaths, element + ".properties", path );
			putToSet( elementToHoconPaths, element + ".json", path );
		    }
		}
	    }
	}

	// pass 2 -- rewrite each HOCON path to fall back to the paths it overlaps
	for ( String path : paths ) {
	    if (BasicMultiPropertiesConfig.isHoconPath( path )) {
		List<String> elements = hoconPathToElementsList.get( path );

		// every HOCON path we overlap (this one included -- filtered out below)
		Set<String> pathSet = new HashSet<String>();
		for( String element : elements ) {
		    // Don't let mutual use of System properties constitute overlap. Nearly every
		    // HOCON path ends in "/", so counting it would make every path overlap every
		    // other and fire this mechanism universally rather than between genuinely
		    // related paths. System properties are also already-resolved leaf values, so
		    // sharing them is no evidence of shared user-authored files.
		    if ( "/".equals( element ) ) continue;

		    // Mirror pass 1's suffix expansion on the lookup side. Checking only the bare
		    // element made overlap asymmetric: given "hocon:/application" and
		    // "hocon:/application.conf", the suffixed path found the bare one but not the
		    // reverse, so the fallback ran in one direction only and the bare path could
		    // still fail to resolve.
		    addHoconPathsFor( elementToHoconPaths, pathSet, element );
		    if ( element.indexOf('.') < 0 ) {
			addHoconPathsFor( elementToHoconPaths, pathSet, element + ".conf" );
			addHoconPathsFor( elementToHoconPaths, pathSet, element + ".properties" );
			addHoconPathsFor( elementToHoconPaths, pathSet, element + ".json" );
		    }
		}
		// Borrow the elements of every overlapping path, walking `paths` (not pathSet)
		// so the borrowings come out in VM-specified order rather than hash order --
		// that is what preserves "a later path in the list wins" among them.
		List<String> newElementsList = new ArrayList<String>();
		for( String orderSettingPath : paths ) {
		    if (BasicMultiPropertiesConfig.isHoconPath( orderSettingPath )) {
			if ( orderSettingPath != path ) { // we add the current path's elements last, since last overrides
			    if ( pathSet.contains( orderSettingPath ) ) {
				newElementsList.addAll( hoconPathToElementsList.get( orderSettingPath ) );
			    }
			}
		    }
		}

		// Own elements last: HOCON merge is last-wins, so whatever this path actually
		// declared still overrides everything it just borrowed.
		newElementsList.addAll( hoconPathToElementsList.get( path ) );
		out.add( makeHoconPathFromElements( newElementsList ) );
	    } else {
		out.add( path );
	    }
	}
	return out;
    }

    private ConfigUtils()
    {}

    /**
     * @deprecated The vmConfig APIs are confusing. Use readUncachedClassloaderResourceConfig(...)
     */
    @Deprecated
    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources )
    { return readUncachedClassloaderResourceConfig( true, defaultResources, preemptingResources, null ); }

    /**
     * @deprecated The vmConfig APIs are confusing. Use readCanonicalDefaultConfig()
     */
    @Deprecated
    public synchronized static MultiPropertiesConfig readVmConfig()
    { return readCanonicalDefaultConfig(); }

    /**
     * @deprecated The vmConfig APIs are confusing. Use readCanonicalDefaultConfig( List delayedLogItemsOut )
     */
    @Deprecated
    public synchronized static MultiPropertiesConfig readVmConfig( List delayedLogItemsOut )
    { return readCanonicalDefaultConfig( delayedLogItemsOut ); }

    /**
     * @deprecated The vmConfig APIs are confusing. Use readUncachedClassloaderResourceConfig(...)
     */
    @Deprecated
    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
    { return readUncachedClassloaderResourceConfig( true, defaultResources, preemptingResources, delayedLogItemsOut); }

    /**
     * @deprecated The vmConfig APIs are confusing. Use foundCanonicalDefaultConfig()
     */
    @Deprecated
    public static synchronized boolean foundVmConfig()
    { return foundCanonicalDefaultConfig(); }

    /*
    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources, MLogger logger)
    {
	List items = new ArrayList();
	MultiPropertiesConfig out = readVmConfig( defaultResources, preemptingResources, items );
	items.addAll( out.getDelayedLogItems() );
	for (Iterator ii = items.iterator(); ii.hasNext(); )
	{
	    DelayedLogItem item = (DelayedLogItem) ii.next();
	    logger.log( item.getLevel(), item.getText(), item.getException() );
	}
	return out;
    }
    */
}
