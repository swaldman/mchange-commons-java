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
 *    <li><p>Paths beginning {@code file:} are file URLs naming properties files at absolute
 *    filesystem locations. This is for configuration you deliberately do <i>not</i> want on the
 *    CLASSPATH, such as credentials. A file that is not found is ignored and forgotten, exactly
 *    as a missing classloader resource is.</p>
 *
 *    <p>A {@code ?} in a file-url path begins a web-url-style query string carrying options.
 *    (Filenames that embed a {@code ?} directly are therefore not supported.) Option <b>keys are
 *    case-sensitive</b>; option <b>values are case-insensitive</b>. Two options are supported:</p>
 *
 *    <ul>
 *      <li><p>{@code permissions=useronly} &mdash; a hygiene check against a credentials file
 *      being left group- or world-readable. The file finally read must be owned by the running
 *      user (or by root) and must have no group or other permission bits: only its owner may hold
 *      read, write or execute permission.</p>
 *
 *      <p>Symbolic links are followed rather than refused, since linking configuration into place
 *      is ordinary practice &mdash; but the whole chain is verified. Every link along the way must
 *      also be owned by the running user or by root, because an attacker cannot {@code chown} a
 *      link to someone else, so a link you own is a link you made. This is what stops a link from
 *      laundering an exposed file, or from quietly redirecting the read to some other file you
 *      happen to own. A cycle of links is refused.</p>
 *
 *      <p>Only the file at the end of the chain has its permission bits examined. A link's own
 *      mode is not a usable signal: it defaults to {@code rwxr-xr-x}, so requiring owner-only bits
 *      would refuse nearly every symbolic link, and on Linux link modes are fixed at
 *      {@code rwxrwxrwx} and ignored by the kernel, with no portable way to change them.
 *      Ownership is the property of a link that means something.</p>
 *
 *      <p>Hard links need no special treatment and get none: a hard link <i>is</i> the file,
 *      sharing its inode, owner and permissions, so it is checked as the regular file it is.</p>
 *
 *      <p><b>Deletion</b> remains possible, and no check on a file can prevent it: whoever can
 *      write a directory above it may unlink the configuration. They cannot substitute content of
 *      their own &mdash; any file they own that you could read would have to carry group or other
 *      bits, and would be refused &mdash; but they can make it vanish, and a file that is absent
 *      and not {@code required} is skipped silently. Because that failure is a quiet one, the
 *      containing directories are walked upward and a <b>warning</b> is logged naming the first
 *      that would permit it: one not owned by the running user or root, or one writable by group
 *      or other without the sticky bit set (the bit that makes {@code /tmp} safe to share). This
 *      is advice and never a veto &mdash; no requirement is imposed on where configuration may
 *      live &mdash; and the remedy it suggests is {@code required=true}, which turns a silent
 *      disappearance into a loud failure.</p>
 *
 *      <p>There also remains an unavoidable gap between the check and the open, since Java offers
 *      no way to interrogate an already-open file; the check resolves the path once and opens what
 *      it resolved, which narrows the gap without closing it.</p></li>
 *
 *      <li>{@code required=true} &mdash; treat the absence of the file as an error rather than
 *      ignoring it. {@code required=false} is the default behavior.</li>
 *    </ul>
 *
 *    <p>Any request these options make that the library cannot honor <b>vetoes</b> the
 *    configuration &mdash; see below. That includes a file failing the permissions check, a link
 *    in its path owned by someone else, a cycle of symbolic links, a mis-cased or unrecognized
 *    option key (so {@code ?Permissions=useronly} is an error, never a silently skipped check), an
 *    option given no value or an unrecognized value, a {@code required=true} file that is absent,
 *    and a platform that cannot report POSIX permissions when {@code permissions} was requested.
 *    What is <i>not</i> a veto: a missing file, absent {@code required=true} &mdash; and that
 *    includes a dangling symbolic link, which is simply absence wearing a link. Absence is not
 *    insecurity.</p></li>
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
 *  <h3>Vetoable configuration</h3>
 *
 *  <p>Most sources can only fail to find configuration. A few can refuse to supply it: a
 *  {@code file:} URL asked to accept only owner-readable files will refuse one that anybody can
 *  read. Such a source implements {@link VetoableConfig} and signals the refusal by throwing
 *  {@link ConfigVetoedException}, which is checked.</p>
 *
 *  <p>The point of making this explicit is that a refusal aborts a read, and a read may include
 *  resource paths the application never chose. The traditional resource-path text files are
 *  supplied by whoever assembles the CLASSPATH, so without care an end user's configuration
 *  choice could stop an application &mdash; or the logging library &mdash; from starting at all.
 *  So whether a veto can reach you is a property of the API you call, decided from the source
 *  class before anything is read:</p>
 *
 *  <ul>
 *    <li><p>{@link MConfig.AsProvidedVetoable} accepts vetoable identifiers, and its methods
 *    declare {@code throws ConfigVetoedException}. This is the only way to read a {@code file:}
 *    URL. Use it when you want to hear about a refusal &mdash; if you asked for a security
 *    property, you generally want to know it could not be honored rather than proceed without
 *    the configuration it guarded.</p></li>
 *
 *    <li><p>{@link MConfig.AsProvided} refuses vetoable identifiers up front, with an
 *    IllegalArgumentException naming them and pointing at {@code AsProvidedVetoable}. Every path
 *    it reads was named by the caller in code, so naming a vetoable one is a programmer error,
 *    and is reported before any file is opened. Note that this follows from the source class
 *    rather than the identifier: a {@code file:} URL is refused even with no query string at
 *    all, since nothing about it could then provoke a veto. That is the price of being able to
 *    decide the question without reading anything.</p></li>
 *
 *    <li><p>{@link MConfig.WithTraditionalDefaultSources} ignores vetoes. The vetoing source is
 *    dropped with a WARNING and every other source still loads. Its resource paths can come from
 *    text files the application does not control, so a veto there is an end user's choice and
 *    must degrade rather than abort.</p></li>
 *  </ul>
 *
 *  <p>Cache entries are keyed by which of these you called, as well as by the resolved resource
 *  paths, because the three do not agree about what reading those paths means.</p>
 *
 *  <h3>Choosing an API</h3>
 *
 *  <p>New code should prefer {@link MConfig.AsProvided}, or
 *  {@link MConfig.AsProvidedVetoable} if it needs {@code file:} URLs. Both read only the
 *  locations you name. {@link MConfig.WithTraditionalDefaultSources} remains, and is not
 *  deprecated, but the resource-path text file machinery it implies is more complex and more
 *  obscure than most applications want. The methods that were traditionally used,
 *  {@code readVmConfig(...)}, remain only for backwards compatibility.</p>
 *
 *  <p>You can choose cached or uncached versions of these approaches. If you read from cached
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

    /**
     *  The ordinary FINE report of an identifier that turned up nothing.
     *
     *  <p>Public so that a source throwing {@link OwnLogCarryingMissingFileException} can supply
     *  the very item its caller would otherwise have supplied, and so report an absence in the
     *  familiar words. Note what the name insists on: this item says the configuration is being
     *  <i>skipped</i>. Do not use it where the absence is not in fact being skipped &mdash; where
     *  it vetoes the read, say &mdash; or the log will contradict what actually happened.</p>
     */
    public static DelayedLogItem skippingFileNotFoundDelayedItem(String rp, Exception e)
    { return new DelayedLogItem( Level.FINE, String.format("The configuration file for resource identifier '%s' could not be found. Skipping. [%s]", rp, e.toString()) ); }

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
