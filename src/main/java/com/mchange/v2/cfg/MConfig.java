package com.mchange.v2.cfg;

import java.util.*;
import com.mchange.v2.log.*;
import com.mchange.v1.cachedstore.*;
import com.mchange.v1.util.ArrayUtils;

/**
 *  MConfig is a facade for defining configuration in a properties-file style,
 *  but where the configuration can be specified as classloader resources
 *  under multiple paths, with sources specified later in a list overriding
 *  sources specified earlier in the list.
 *
 *  Traditionally, the list of places in which properties-style config
 *  can live could itself be specified as a classloader resources. The
 *  library would, in order, look for all of
 *
 *  - `/com/mchange/v2/cfg/vmConfigResourcePaths.txt`
 *  - `/com/mchange/v2/cfg/defaultConfigResourcePaths.txt`
 *  - `/mchange-config-resource-paths.txt`
 *
 *  It would treat all non-empty lines not beginning in `#` character as
 *  a classloader resource path in which config might be found.
 *  Config source from later in this list would take preference over
 *  config sources earlier. So if `/com/mchange/v2/cfg/vmConfigResourcePaths.txt`
 *  contained the path `/early.properties` and `/mchange-config-resource-paths.txt`
 *  contained the path `/late.properties`, and a property was specified in both
 *  files, the specification in `late.properties` would win.
 *
 *  If NONE of the resources specified by the paths above exist, or
 *  no paths are parsed from them, then the library would assume configuration
 *  could be found in the following default sources:
 *
 *  - `/mchange-commons.properties`
 *  - `hocon:/reference,/application,/`
 *  - `/`
 *
 *  As always, should they contain any conflicting config, definitions
 *  in later resources take preference over early resources.
 *
 *  Note that there are some special kinds of paths in these defaults
 *  that are not simple classloader resource paths:
 *
 *  - `/` is a special token that means System properties
 *  - paths beginning `hocon:` are ignored if [HOCON / lightbend config](https://github.com/lightbend/config/blob/main/HOCON.md) libraries
 *    are not available on the CLASSPATH, but are interpreted according to HOCON conventions if they are.
 *    So the specification `hocon:/reference,/application,/` would expand to include all of `reference.properties`,
 *    `reference.json`, `reference.conf`, `application.properties`, `application.json`, `application.conf`,
 *    in that order, with later sources raking preference over earlier elements. Plus, with the special path `application`,
 *    items referred to by System properties `config.file`, or, if that is not present, `config.url` may be loaded
 *    instead of `application.properties` / `application.json` / `application.conf`.
 *
 *  Note that, counterintuitively, those hardcoded backstop config locations...
 *
 *  - `/mchange-commons.properties`
 *  - `hocon:/reference,/application,/`
 *  - `/`
 *
 *  are in effect **whenever the built-in default resource-path locations** supply no resource
 *  paths, even if the developer explicity provides their own "preemptingResources" and "defaultResources"
 *  config locations. Traditionally, the only way to prevent config from checking the
 *  default locations was to ensure that at least one of
 *
 *  - `/com/mchange/v2/cfg/vmConfigResourcePaths.txt`
 *  - `/com/mchange/v2/cfg/defaultConfigResourcePaths.txt`
 *  - `/mchange-config-resource-paths.txt`
 *
 *  supplied resource paths. (And `/com/mchange/v2/cfg/defaultConfigResourcePaths.txt` is in fact a recent addition to the list.)
 *
 *  On the theory that this is quite a lot of complexity (and potentially attack surface)
 *  for specifying config resource locations, there is now a new "as-provided" API.
 *  It accepts a list (or if you prefer, two lists, `defaultResources` and `preemptingResources`)
 *  of config locations, and the most recent location "wins" if there are conflicting properties.
 *  Only the config locations you specify are checked, although if you include the special forms,
 *  then System properties and the menagerie of resources that lightbend config examines may be
 *  invoked.
 *
 *  You can access the traditional functionality of this library via the `MConfig.WithTraditionalDefaultDataSources.*`
 *  methods and the simpler as-provided functionality via `MConfig.AsProvided.*` methods. The methods that were
 *  traditionally used, `readVmConfig(...)`, remain, but only for backwards compatability.
 *
 *  You can choose cached or uncached versions of both approaches. If you read from cached methods, the config
 *  sources will only be read once even if you call the same method multiple times. It may be simpler to
 *  use the uncached methods if your application will be caching the configuration itself.
 */
public final class MConfig
{
    private final static String[] EMPTY_STRING_ARRAY = new String[0];

    private final static MLogger logger = MLog.getLogger( MConfig.class );

    private final static Map<DelayedLogItem.Level,MLevel> levelMap;

    static
    {
	try
	{
	    Map<DelayedLogItem.Level,MLevel> lm = new HashMap();
	    for( DelayedLogItem.Level level : DelayedLogItem.Level.values() )
		lm.put( level, (MLevel) (MLevel.class.getField( level.toString() ).get( null )) );
	    levelMap = Collections.unmodifiableMap( lm );
	}
	catch ( RuntimeException e )
	    {
		e.printStackTrace();
		throw e;
	    }
	catch ( Exception e )
	    {
		e.printStackTrace();
		throw new RuntimeException( e );
	    }
    }

    final static CachedStore cache = CachedStoreUtils.synchronizedCachedStore( CachedStoreFactory.createNoCleanupCachedStore( new CSManager() ) );

    public final static class WithTraditionalDefaultSources {

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources )
        { return ConfigUtils.readUncachedClassloaderResourceConfig( true, defaultResources, preemptingResources, null ); }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
        { return ConfigUtils.readUncachedClassloaderResourceConfig( true, defaultResources, preemptingResources, delayedLogItemsOut); }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
        {
            try
                {
                    // we want to collect any delayed log items emitted by ConfigUtils.condenseResources(...)
                    List dlioEffective = (delayedLogItemsOut == null ? new ArrayList() : delayedLogItemsOut);
                    String[] resourcePaths = ConfigUtils.condenseResources(true, defaultResources, preemptingResources, dlioEffective);
                    return (MultiPropertiesConfig) cache.find( new PathsKey( resourcePaths, dlioEffective ) );
                }
            catch (CachedStoreException e)
            { throw new RuntimeException( e ); }
        }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources)
        { return readCachedClassloaderResourceConfig( defaultResources, preemptingResources, null); }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig()
        { return readCachedClassloaderResourceConfig( ConfigUtils.NO_PATHS, ConfigUtils.NO_PATHS ); }

        private WithTraditionalDefaultSources() {}
    }

    public final static class AsProvided {

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
        {
            try
                {
                    // we want to collect any delayed log items emitted by ConfigUtils.condenseResources(...)
                    List dlioEffective = (delayedLogItemsOut == null ? new ArrayList() : delayedLogItemsOut);
                    String[] resourcePaths = ConfigUtils.condenseResources(false, defaultResources, preemptingResources, dlioEffective);
                    return (MultiPropertiesConfig) cache.find( new PathsKey( resourcePaths, dlioEffective ) );
                }
            catch (CachedStoreException e)
            { throw new RuntimeException( e ); }
        }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResourcess, String[] preemptingResources)
        { return readCachedClassloaderResourceConfig( defaultResourcess, preemptingResources, null ); }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig( String[] resourcePaths, List delayedLogItemsOut )
        { return readCachedClassloaderResourceConfig(EMPTY_STRING_ARRAY, resourcePaths, delayedLogItemsOut); }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig( String[] resourcePaths )
        { return AsProvided.readCachedClassloaderResourceConfig( resourcePaths, (List) null ); }
    }

    /**
     *  Later entries in the configs array override earlier entries.
     */
    public static MultiPropertiesConfig combine( MultiPropertiesConfig[] configs )
    { return ConfigUtils.combine( configs ); }

    public static void dumpToLogger(List<DelayedLogItem> items, MLogger logger)
    { for( DelayedLogItem item : items ) dumpToLogger( item, logger ); }

    public static void dumpToLogger( DelayedLogItem item, MLogger logger )
    { logger.log( levelMap.get( item.getLevel() ), item.getText(), item.getException() ); }

    private final static class PathsKey
    {
	String[] paths;
	List     delayedLogItems;

	public boolean equals(Object o)
	{ 
	    if (o instanceof PathsKey)
		return Arrays.equals( paths, ((PathsKey) o).paths );
	    else
		return false;
	}

	public int hashCode()
	{ return ArrayUtils.hashArray( paths ); }

        // it's fine for delayedLogItems to be null
	PathsKey(String[] paths, List delayedLogItems)
	{
	    this.delayedLogItems = delayedLogItems;
	    this.paths = paths;
	}
    }

    private static class CSManager implements CachedStore.Manager
    {
	public boolean isDirty(Object key, Object cached) throws Exception
	{ return false; }

	public Object recreateFromKey(Object key) throws Exception
	{
	    PathsKey pk = (PathsKey) key;

	    /*
	    for( Iterator ii = pk.delayedLogItems.iterator(); ii.hasNext(); )
	    {
		DelayedLogItem pm = (DelayedLogItem) ii.next();
		logger.log( pm.getLevel(), pm.getText(), pm.getException() );
	    }
	    */

	    List<DelayedLogItem> items = new ArrayList<DelayedLogItem>();
            if (pk.delayedLogItems != null) items.addAll( pk.delayedLogItems );
	    Object out =  ConfigUtils.read( pk.paths, items );
	    dumpToLogger( items, logger );
	    return out;
	}
    }

    private MConfig()
    {}

    /**
     * @deprecated The vmConfig APIs are confusing. Use MConfig.WithTraditionalDefaultSources.readCachedClassloaderResourceConfig(...)
     */
    public static MultiPropertiesConfig readVmConfig(String[] defaults, String[] preempts)
    { return MConfig.WithTraditionalDefaultSources.readCachedClassloaderResourceConfig( defaults, preempts); }

    /**
     * @deprecated The vmConfig APIs are confusing. Use MConfig.WithTraditionalDefaultSources.readCachedClassloaderRewsourceConfig(...)
     */
    public static MultiPropertiesConfig readVmConfig()
    { return MConfig.WithTraditionalDefaultSources.readCachedClassloaderResourceConfig(); }

    /**
     * @deprecated This API is confusingly nonspecifc. Use Use MConfig.AsProvided.readCachedClassloaderResourceConfig( String[] resourcePaths )
     */
    public static MultiPropertiesConfig readConfig( String[] resourcePaths )
    { return MConfig.AsProvided.readCachedClassloaderResourceConfig( resourcePaths ); }
}
