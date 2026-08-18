package com.mchange.v2.cfg;

import java.util.List;
import java.util.Properties;

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
 *   <li>{@code /com/mchange/v2/cfg/defaultConfigResourcePaths.txt}</li>
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
    private static String PROGRAMMATICALLY_SUPPLIED_PROPERTIES = "PROGRAMMATICALLY_SUPPLIED_PROPERTIES";

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
    { return fromProperties( PROGRAMMATICALLY_SUPPLIED_PROPERTIES, props ); }

    public abstract String[] getPropertiesResourcePaths();

    public abstract Properties getPropertiesByResourcePath(String path);


    /**
     *  The special prefix "" returns all the Properties
     */
    public abstract Properties getPropertiesByPrefix(String pfx);

//    public abstract Properties getProperties( String key );

    public abstract String getProperty( String key );

    public abstract List getDelayedLogItems();
}
