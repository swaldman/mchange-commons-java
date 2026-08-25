package com.mchange.v2.cfg;

import java.util.*;
import com.mchange.v2.log.*;
import com.mchange.v1.cachedstore.*;
import com.mchange.v1.util.ArrayUtils;

import static com.mchange.v2.cfg.DelayedLogItem.*;

/**
 *  MConfig is a facade for defining configuration in a properties-file style,
 *  but where the configuration can be specified as classloader resources
 *  under multiple paths, with sources specified later in a list overriding
 *  sources specified earlier in the list.
 *
 *  <h3>The traditional API</h3>
 *
 *  <p>Traditionally, the list of places in which properties-style config
 *  can live could itself be specified as a classloader resource. The
 *  library would, in order, look for all of</p>
 *
 *  <ul>
 *    <li>{@code /com/mchange/v2/cfg/vmConfigResourcePaths.txt}</li>
 *    <li>{@code /com/mchange/v2/cfg/defaultConfigResourcePaths.txt}</li>
 *    <li>{@code /mchange-config-resource-paths.txt}</li>
 *  </ul>
 *
 *  <p>It would treat all non-empty lines not beginning in {@code #} character as
 *  a classloader resource path in which config might be found.
 *  Config sources later in this list take preference over
 *  config sources earlier. So if {@code /com/mchange/v2/cfg/vmConfigResourcePaths.txt}
 *  contained the path {@code /early.properties} and {@code /mchange-config-resource-paths.txt}
 *  contained the path {@code /late.properties}, and a property was specified in both
 *  files, the specification in {@code late.properties} would win.</p>
 *
 *  <p>If NONE of the resources specified by the paths above exist, or
 *  no paths are parsed from them, then the library would assume configuration
 *  could be found in the following default sources:</p>
 *
 *  <ul>
 *    <li>{@code /mchange-commons.properties}</li>
 *    <li>{@code hocon:/reference,/application,/}</li>
 *    <li>{@code /}</li>
 *  </ul>
 *
 *  <p>As always, should they contain any conflicting config, definitions
 *  in later resources take preference over early resources.</p>
 *
 *  <h3>Special resource paths</h3>
 *
 *  <p>Note that there are some special kinds of paths in these defaults
 *  that are not simple classloader resource paths:</p>
 *
 *  <ul>
 *    <li><p>{@code /} is a special token that means System properties.</p></li>
 *
 *    <li><p>Paths beginning {@code hocon:} are interpreted according to
 *    <a href="https://github.com/lightbend/config/blob/main/HOCON.md">HOCON</a> conventions when
 *    HOCON / lightbend config libraries
 *    are available on the CLASSPATH. When those libraries are absent, such a path contributes no
 *    configuration: it is skipped with a FINE-level log item naming the path as written, and the
 *    remaining sources are read normally. (One exception: if a resource happens to exist at the
 *    path you get by chopping {@code hocon:} off the identifier, the skip is escalated to a WARNING
 *    carrying an exception, rather than a quiet FINE.)</p>
 *
 *    <p>So the specification {@code hocon:/reference,/application,/} would expand to include all of
 *    {@code reference.properties},
 *    {@code reference.json}, {@code reference.conf}, {@code application.properties},
 *    {@code application.json}, {@code application.conf},
 *    in that order, with later sources taking preference over earlier elements. Plus, with the
 *    special path {@code application},
 *    items referred to by System properties {@code config.resource}, {@code config.file}, or
 *    {@code config.url} &mdash; checked in
 *    that order, the first one set winning &mdash; may be loaded instead of
 *    {@code application.properties} / {@code application.json} / {@code application.conf}.</p></li>
 *  </ul>
 *
 *  <h3>The hardcoded backstop</h3>
 *
 *  <p>Note that, counterintuitively, those hardcoded backstop config locations...</p>
 *
 *  <ul>
 *    <li>{@code /mchange-commons.properties}</li>
 *    <li>{@code hocon:/reference,/application,/}</li>
 *    <li>{@code /}</li>
 *  </ul>
 *
 *  <p>...are in effect <b>whenever the built-in default resource-path locations</b> supply no resource
 *  paths, even if the developer explicitly provides their own {@code preemptingResources} and
 *  {@code defaultResources}
 *  config locations. Traditionally, the only way to prevent config from checking the
 *  default locations was to ensure that at least one of</p>
 *
 *  <ul>
 *    <li>{@code /com/mchange/v2/cfg/vmConfigResourcePaths.txt}</li>
 *    <li>{@code /com/mchange/v2/cfg/defaultConfigResourcePaths.txt}</li>
 *    <li>{@code /mchange-config-resource-paths.txt}</li>
 *  </ul>
 *
 *  <p>supplied resource paths. (And {@code /com/mchange/v2/cfg/defaultConfigResourcePaths.txt}
 *  is in fact a recent addition to the list.)</p>
 *
 *  <h3>The as-provided API</h3>
 *
 *  <p>On the theory that this is quite a lot of complexity (and potentially attack surface)
 *  for specifying config resource locations, there is now a new "as-provided" API.
 *  It accepts a list (or if you prefer, two lists, {@code defaultResources} and
 *  {@code preemptingResources})
 *  of config locations, and the most recent location "wins" if there are conflicting properties.
 *  Only the config locations you specify are checked, although if you include the special forms,
 *  then System properties and the menagerie of resources that lightbend config examines may be
 *  invoked.</p>
 *
 *  <h3>Choosing an API</h3>
 *
 *  <p>You can access the traditional functionality of this library via the
 *  {@link MConfig.WithTraditionalDefaultSources}
 *  methods and the simpler as-provided functionality via {@link MConfig.AsProvided} methods.
 *  The methods that were
 *  traditionally used, {@code readVmConfig(...)}, remain, but only for backwards compatibility.</p>
 *
 *  <p>You can choose cached or uncached versions of both approaches. If you read from cached
 *  methods, the config
 *  sources will only be read once even if you call the same method multiple times. It may be
 *  simpler to
 *  use the uncached methods if your application will be caching the configuration itself.</p>
 */
public final class MConfig
{
    // we need this lazy, because of the risk of cycles
    // between the logging library and this one.
    //
    // if the MLog is touched before this library,
    // it tries to look up config in MLog class init,
    // this class tries to fetch a logger in its class init,
    // while MLog's class init is still in process,
    // provoking a NullPointerException there from the unexpected
    // re-entrancy. Lazy construction of the our logger breaks this;
    // the MConfig class can be initialized as a response to MLog
    // initializing without unexpectedly re-entering config.
    private static MLogger _logger = null;

    // note that we need for this NOT to be hit during MLog's class init.
    // MLog reads from the uncached path, which never hits this logger.
    // we should keep it that way.
    synchronized static MLogger logger()
    {
        if (_logger == null) _logger = MLog.getLogger( MConfig.class );
        return _logger;
    }

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

    enum Kind {
        Traditional,
        AsProvided,
        AsProvidedVetoable
    }

    final static CachedStore cache = CachedStoreUtils.synchronizedCachedStore( CachedStoreFactory.createNoCleanupCachedStore( new CSManager() ) );

    public final static class WithTraditionalDefaultSources {

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources )
        { return readUncachedClassloaderResourceConfig( defaultResources, preemptingResources, null ); }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
        {
            try { return readForKind( Kind.Traditional, defaultResources, preemptingResources, delayedLogItemsOut); }
            catch (ConfigVetoedException e)
            { throw new RuntimeException("BUG! MConfig.readForKind(...) with Kind.Traditional should never throw a ConfigVetoedException.", e ); }
        }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
        {
            try
                {
                    // we want to collect any delayed log items emitted by ConfigUtils.condenseResources(...)
                    List dlioEffective = (delayedLogItemsOut == null ? new ArrayList() : delayedLogItemsOut);
                    String[] resourcePaths = ConfigUtils.condenseResources(true, defaultResources, preemptingResources, dlioEffective);
                    return (MultiPropertiesConfig) cache.find( new PathsKey( resourcePaths, Kind.Traditional, dlioEffective ) );
                }
            catch (CachedStoreException e)
            { throw new RuntimeException( e ); }
        }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources)
        { return readCachedClassloaderResourceConfig( defaultResources, preemptingResources, null); }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig()
        { return readCachedClassloaderResourceConfig( ConfigUtils.EMPTY_STRING_ARRAY, ConfigUtils.EMPTY_STRING_ARRAY ); }

        private WithTraditionalDefaultSources() {}
    }

    public final static class AsProvided {

        private static void requireNoVetoableConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
        {
            String[] vetoableConfig = ConfigUtils.vetoableConfigFrom( defaultResources, preemptingResources, delayedLogItemsOut );
            if (vetoableConfig.length != 0)
                throw new IllegalArgumentException( "At least one identifier contains vetoable config. Use MConfig.AsProvidedVetoable to support these resources: " + Arrays.toString(vetoableConfig) );
        }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
        {
            requireNoVetoableConfig( defaultResources, preemptingResources, delayedLogItemsOut );
            try
                {
                    // we want to collect any delayed log items emitted by ConfigUtils.condenseResources(...)
                    List dlioEffective = (delayedLogItemsOut == null ? new ArrayList() : delayedLogItemsOut);
                    String[] resourcePaths = ConfigUtils.condenseResources(false, defaultResources, preemptingResources, dlioEffective);
                    return (MultiPropertiesConfig) cache.find( new PathsKey( resourcePaths, Kind.AsProvided, dlioEffective ) );
                }
            catch (CachedStoreException e)
            { throw new RuntimeException( e ); }
        }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources)
        { return readCachedClassloaderResourceConfig( defaultResources, preemptingResources, null ); }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig( String[] resourcePaths, List delayedLogItemsOut )
        { return readCachedClassloaderResourceConfig(ConfigUtils.EMPTY_STRING_ARRAY, resourcePaths, delayedLogItemsOut); }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig( String[] resourcePaths )
        { return AsProvided.readCachedClassloaderResourceConfig( resourcePaths, (List) null ); }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut)
        {
            try
            {
                requireNoVetoableConfig( defaultResources, preemptingResources, delayedLogItemsOut );
                return readForKind( Kind.AsProvided, defaultResources, preemptingResources, delayedLogItemsOut );
            }
            catch (ConfigVetoedException e)
            { throw new RuntimeException( "BUG! MConfig.readForKind(...) with Kind.AsProvided should never throw a ConfigVetoedException.", e ); }
        }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources)
        { return readUncachedClassloaderResourceConfig( defaultResources, preemptingResources, null ); }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig( String[] resourcePaths, List delayedLogItemsOut )
        { return readUncachedClassloaderResourceConfig( ConfigUtils.EMPTY_STRING_ARRAY, resourcePaths, delayedLogItemsOut ); }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig( String[] resourcePaths )
        { return AsProvided.readUncachedClassloaderResourceConfig( resourcePaths, (List) null ); }

        private AsProvided() {}
    }

    public final static class AsProvidedVetoable {

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut) throws ConfigVetoedException
        {
            try
                {
                    // we want to collect any delayed log items emitted by ConfigUtils.condenseResources(...)
                    List dlioEffective = (delayedLogItemsOut == null ? new ArrayList() : delayedLogItemsOut);
                    String[] resourcePaths = ConfigUtils.condenseResources(false, defaultResources, preemptingResources, dlioEffective);
                    return (MultiPropertiesConfig) cache.find( new PathsKey( resourcePaths, Kind.AsProvidedVetoable, dlioEffective ) );
                }
            catch (CachedStoreException e)
            {
                if (e.getCause() instanceof ConfigVetoedException )
                    throw (ConfigVetoedException) e.getCause();
                else
                    throw new RuntimeException( e );
            }
        }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources) throws ConfigVetoedException
        { return readCachedClassloaderResourceConfig( defaultResources, preemptingResources, null ); }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig( String[] resourcePaths, List delayedLogItemsOut ) throws ConfigVetoedException
        { return readCachedClassloaderResourceConfig(ConfigUtils.EMPTY_STRING_ARRAY, resourcePaths, delayedLogItemsOut); }

        public static MultiPropertiesConfig readCachedClassloaderResourceConfig( String[] resourcePaths ) throws ConfigVetoedException
        { return AsProvidedVetoable.readCachedClassloaderResourceConfig( resourcePaths, (List) null ); }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources, List delayedLogItemsOut) throws ConfigVetoedException
        { return readForKind( Kind.AsProvidedVetoable, defaultResources, preemptingResources, delayedLogItemsOut ); }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig(String[] defaultResources, String[] preemptingResources) throws ConfigVetoedException
        { return readUncachedClassloaderResourceConfig( defaultResources, preemptingResources, null ); }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig( String[] resourcePaths, List delayedLogItemsOut ) throws ConfigVetoedException
        { return readUncachedClassloaderResourceConfig( ConfigUtils.EMPTY_STRING_ARRAY, resourcePaths, delayedLogItemsOut ); }

        public static MultiPropertiesConfig readUncachedClassloaderResourceConfig( String[] resourcePaths ) throws ConfigVetoedException
        { return AsProvidedVetoable.readUncachedClassloaderResourceConfig( resourcePaths, (List) null ); }

        private AsProvidedVetoable() {}
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
        Kind     kind;
	List     delayedLogItems;

	public boolean equals(Object o)
	{ 
	    if (o instanceof PathsKey)
            {
                PathsKey other = (PathsKey) o;
		return Arrays.equals( this.paths, other.paths ) && this.kind.equals(other.kind);
            }
	    else
		return false;
	}

	public int hashCode()
	{
            int out = ArrayUtils.hashArray( paths );
            out ^= kind.hashCode();
            return out;
        }

        // it's fine for delayedLogItems to be null
	// PathsKey(String[] paths, List delayedLogItems)
        // { this( paths, false, delayedLogItems ); }

        PathsKey(String[] paths, Kind kind, List delayedLogItems)
	{
	    this.delayedLogItems = delayedLogItems;
            this.kind  = kind;
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
	    Object out = null;
            ConfigVetoedException cve = null;
            try
                {
                    out = readForKind(pk.kind, pk.paths, items);
                }
            catch (ConfigVetoedException e)
                {
                    cve = e;
                    items.add( new DelayedLogItem( Level.WARNING, "Configuration was vetoed.", e ) );
                }
	    dumpToLogger( items, logger() );
            if (cve != null)
                throw cve;
            else
                return out;
	}
    }


    static MultiPropertiesConfig readForKind(Kind kind, String[] defaultResources, String[] preemptingResources, List delayedLogItems) throws ConfigVetoedException
    {
        if (kind == Kind.Traditional)
        {
            String[] resourcePath = ConfigUtils.condenseResources( true, defaultResources, preemptingResources, delayedLogItems );
            return readForKind( kind, resourcePath, delayedLogItems );
        }
        else // AsProvided, AsProvidedVetoable
        {
            String[] resourcePath = ConfigUtils.condenseResources( false, defaultResources, preemptingResources, delayedLogItems );
            return readForKind( kind, resourcePath, delayedLogItems );
        }
    }

    static MultiPropertiesConfig readForKind(Kind kind, String[] resourcePath, List delayedLogItems) throws ConfigVetoedException
    {
        if (kind == Kind.AsProvided || kind == Kind.Traditional)
            return new BasicMultiPropertiesConfig( kind, resourcePath, delayedLogItems );
        else
            return new BasicMultiPropertiesConfig(BasicMultiPropertiesConfig.VetoThrowing.INSTANCE, resourcePath, delayedLogItems);
    }

    private MConfig()
    {}

    /**
     * @deprecated The vmConfig APIs are confusing. Use
     * {@link MConfig.WithTraditionalDefaultSources#readCachedClassloaderResourceConfig(String[], String[])}
     */
    @Deprecated
    public static MultiPropertiesConfig readVmConfig(String[] defaults, String[] preempts)
    { return MConfig.WithTraditionalDefaultSources.readCachedClassloaderResourceConfig( defaults, preempts); }

    /**
     * @deprecated The vmConfig APIs are confusing. Use
     * {@link MConfig.WithTraditionalDefaultSources#readCachedClassloaderResourceConfig()}
     */
    @Deprecated
    public static MultiPropertiesConfig readVmConfig()
    { return MConfig.WithTraditionalDefaultSources.readCachedClassloaderResourceConfig(); }

    /**
     * @deprecated This API is confusingly nonspecific. Use
     * {@link MConfig.AsProvided#readCachedClassloaderResourceConfig(String[])}
     */
    @Deprecated
    public static MultiPropertiesConfig readConfig( String[] resourcePaths )
    { return MConfig.AsProvided.readCachedClassloaderResourceConfig( resourcePaths ); }
}
