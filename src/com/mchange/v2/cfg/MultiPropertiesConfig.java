package com.mchange.v2.cfg;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import com.mchange.v2.uid.UidUtils;

/**
 * MultiPropertiesConfig allows applications to accept configuration data
 * from a more than one property file (each of which is to be loaded from
 * a unique path using this class' ClassLoader's resource-loading mechanism),
 * and permits access to property data via the resource path from which the
 * properties were loaded, via the prefix of the property (where hierarchical 
 * property names are presumed to be '.'-separated), and simply by key.
 * In the by-key and by-prefix indices, when two definitions conflict, the
 * key value pairing specified in the MOST RECENT properties file shadows
 * earlier definitions, and files are loaded in the order of the list of
 * resource paths provided a constructor.
 *
 * The resource path "/" is a special case that always refers to System
 * properties. No actual resource will be loaded.
 *
 * If a HOCON implementation (lightbend/typesafe config, detected by probing for
 * {@code com.typesafe.config.Config}) is available on the CLASSPATH, resource paths
 * specified as "hocon:/path/to/resource" will be parsed as
 * <a href="https://github.com/lightbend/config/blob/main/HOCON.md">HOCON</a>,
 * whenever values can be interpreted as Strings. When no such implementation is
 * present, those paths are skipped with a FINE-level log item.
 *
 * <p>Traditionally the library resolved a default ("vmConfig") list of resource paths
 * from text files, themselves ClassLoader-managed resources, read and concatenated in
 * this order:
 *
 * <ul>
 *   <li>{@code /com/mchange/v2/cfg/vmConfigResourcePaths.txt}</li>
 *   <li>{@code /mchange-config-resource-paths.txt}</li>
 * </ul>
 *
 * Each file should be one resource path per line, with blank lines ignored and lines
 * beginning with '#' treated as comments.
 *
 * <p>If none of those text files is available, or none of them yields any path, the
 * following resources are checked instead:
 * "/mchange-commons.properties", "hocon:/reference,/application,/", "/"
 *
 * <p><b>The {@link com.mchange.v2.cfg.MConfig} facade is the authoritative documentation
 * for how config sources are discovered, layered, and prioritized, and is the supported
 * entry point for acquiring configuration.</b> The {@code readVmConfig} methods on this
 * class are deprecated; note also that they resolve paths by a different algorithm and
 * cache into a different store than the identically named methods on {@link MConfig}.
 *
 * See <a href="../../v3/hocon/HoconPropertiesConfigSource.html">HoconPropertiesConfigSource</a> for information
 * on HOCON identifiers.
 */
public abstract class MultiPropertiesConfig implements PropertiesConfig
{
    private final static String PROGRAMMATICALLY_SUPPLIED_PROPERTIES_PFX = "PROGRAMMATICALLY_SUPPLIED_PROPERTIES-";

    /**
     * @deprecated Please use the MConfig facade class to acquire configuration
     */
    @Deprecated
    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources )
    { return ConfigUtils.readVmConfig( defaultResources, preemptingResources ); }

    /**
     * @deprecated Please use the MConfig facade class to acquire configuration
     */
    @Deprecated
    public static MultiPropertiesConfig readVmConfig()
    { return ConfigUtils.readVmConfig(); }

    public static MultiPropertiesConfig fromProperties(String notionalResourcePath, Properties props)
    { return new BasicMultiPropertiesConfig( notionalResourcePath, props ); }

    public static MultiPropertiesConfig fromProperties(Properties props)
    { return fromProperties( PROGRAMMATICALLY_SUPPLIED_PROPERTIES_PFX + UidUtils.allocateWithinVmSequential(), props ); }

    public abstract String[] getPropertiesResourcePaths();

    /** @return an empty Properties, even if path is unknown, just to avoid downstream NullPointerExceptions */
    public abstract Properties getPropertiesByResourcePath(String path);

    /**
     *  The Properties beneath a prefix, <b>keyed by full key</b> rather than by the remainder
     *  after the prefix. {@code getPropertiesByPrefix("a.b")} over a config holding
     *  {@code a.b.c=1} returns {@code {a.b.c=1}}, not {@code {c=1}}.
     *
     *  <p>A prefix matches by <b>dot-separated token</b>, never by character. So {@code "a.b"}
     *  reaches {@code a.b.c} and {@code a.b.c.d}, but not {@code a.bx} or {@code a.b-x}, which
     *  merely begin with the same letters. This is what the {@link PropertiesConfig} declaration
     *  means by asking that a prefix be a complete token.</p>
     *
     *  <p>The prefix <b>includes the exact key</b>. A config holding only {@code a.b=v} answers
     *  {@code getPropertiesByPrefix("a.b")} with {@code {a.b=v}}, not with an empty Properties:
     *  every key is a prefix of itself. The consequence worth knowing is that a non-empty result
     *  does not by itself mean the prefix names a populated scope &mdash; it may be one bare key
     *  wearing the scope's name. Where the distinction matters, ask for it:</p>
     *
     *  <pre>
     *    Properties under  = cfg.getPropertiesByPrefix( pfx );
     *    boolean bareKeySet = under.containsKey( pfx );          // or getProperty(pfx) != null
     *    boolean hasSubkeys = under.size() &gt; (bareKeySet ? 1 : 0);
     *  </pre>
     *
     *  <p>The special prefix {@code ""} returns all the Properties. A key with no dot in it is
     *  reachable only under {@code ""} and under itself.</p>
     *
     *  <p>Implementors: the exact key belongs in the result. The in-library consumer that walks
     *  a prefix's keys is {@code WhitelistManager}, whose fallback path over live System
     *  properties spells the same rule out as
     *  {@code k.equals(baseKey) || k.startsWith(baseKey + ".")}. An implementation that filters
     *  on {@code pfx + "."} alone will disagree with {@code BasicMultiPropertiesConfig}, and so
     *  with the result of {@link MConfig#combine}, which flattens everything it is given into
     *  one of those.</p>
     *
     *  @return an empty Properties, even if prefix is unknown, just to avoid downstream NullPointerExceptions
     */
    @Override
    public abstract Properties getPropertiesByPrefix(String pfx);

//    public abstract Properties getProperties( String key );

    @Override
    public abstract String getProperty( String key );

    public abstract List<DelayedLogItem> getDelayedLogItems();

    public abstract boolean wasRead(String resourcePath);
    public abstract boolean wasVetoed(String resourcePath);
    public abstract boolean wasNotFound(String resourcePath);
    public abstract boolean wasFault(String resourcePath);
    public abstract Set<String> getAllRead();
    public abstract Set<String> getAllVetoed();
    public abstract Set<String> getAllNotFound();
    public abstract Set<String> getAllFaults();

    public boolean wasEncountered(String resourcePath)
    { return wasRead(resourcePath) || wasVetoed(resourcePath) || wasNotFound(resourcePath) || wasFault(resourcePath); }
}
