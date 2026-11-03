package com.mchange.v2.log;

import java.io.*;
import java.util.*;

import java.lang.reflect.Method;
import java.net.URL;
import com.mchange.v2.cfg.MConfig;
import com.mchange.v2.cfg.MultiPropertiesConfig;
import com.mchange.v2.cfg.DelayedLogItem;
import com.mchange.v2.cfg.SealedSystemProperties;
import com.mchange.v2.cfg.SealedSystemPropertiesBooleanProperty;

import static com.mchange.v2.cfg.DelayedLogItem.Level;

public final class MLogConfig
{
    public final static String FREEZE_CONFIG_KEY = "com.mchange.v2.log.freezeConfig";

    // If this resource is present, it's expected to be a list of
    // notional resource paths of the sort decodable by com.mchange.v2.cfg APIs.
    // values found in later resources will shadow values found in earlier resources.
    //
    // If the resource is multiply present on the CLASSPATH, resources found EARLIER in the CLASSPATH
    // will, according the JVM-usual conventions, take precedence over resources found LATER in the CLASSPATH.
    //
    // A bit confusingly, what this adds up to is
    //
    //   (1) The final resource path list preserves the in-file order of resource paths for
    //       each `mlog-config-resource-paths.txt` found
    //
    //   (2) Lists are combined in reverse order: The list found first in the CLASSPATH
    //       represents the TAIL of the ultimate resource path list constructed
    public final static String MLOG_CONFIG_RESOURCE_PATHS_RESOURCE = "mlog-config-resource-paths.txt";

    private final static MultiPropertiesConfig[] EMPTY_MPC_ARRAY = new MultiPropertiesConfig[0];

    private final static String STANDARD_CFG_DEFAULT_MCHANGE_LOG     = "/com/mchange/v2/log/default-mchange-log.properties";
    private final static String STANDARD_CFG_MCHANGE_COMMONS         = "/mchange-commons.properties";
    private final static String STANDARD_CFG_MCHANGE_LOG             = "/mchange-log.properties";

    // MT: all now mutable references, protected by class' lock
    private static MultiPropertiesConfig                 config                = null;
    private static List<DelayedLogItem>                  bootstrapLogItems     = null;
    private static Method                                delayedDumpToLogger   = null;
    private static SealedSystemPropertiesBooleanProperty freezeConfigProperty  = null; // only initialized after log package initialization completes
    private static MLogger                               classLogger           = null; // only initialized after log package initialization completes
    private static boolean                               refreshDebugWarned    = false;
    private static List<String>                          resourcePathsTxtPaths = null;

    private static List<String> resourcePathsFromMLogConfigResourcePathsTxt(List<DelayedLogItem> delayedLogItemsOut)
    {
        List<String> out = null;

        try
        {
            ArrayList<URL> allResources = Collections.list(MLogConfig.class.getClassLoader().getResources(MLOG_CONFIG_RESOURCE_PATHS_RESOURCE));

            int sz = allResources.size();

            if (sz > 0)
            {
                out = new ArrayList<>();

                // we want earlier entries in the CLASSPATH to have priority, so we start
                // at the last element and concatenate as we work backwards
                for (int i = sz; --i >= 0;)
                {
                    URL resource = allResources.get(i);
                    BufferedReader br = new BufferedReader( new InputStreamReader( resource.openStream(), "UTF8" ) );
                    try
                    {
                        List<String> tmp = new ArrayList<>();
                        String line;
                        while ((line = br.readLine()) != null)
                        {
                            String trimmed = line.trim();
                            if (trimmed.length() > 0 && !trimmed.startsWith("#"))
                            {
                                // the FIRST whitespace-preceded '#', not the first '#': a line may carry both a
                                // literal '#' and a trailing comment
                                int cut_point = -1;
                                for (int j = 1, len = trimmed.length(); cut_point < 0 && j < len; ++j)
                                    if (trimmed.charAt(j) == '#' && Character.isWhitespace(trimmed.charAt(j-1)))
                                        cut_point = j;

                                // we only interpret as a comment if we have whitespace before the #, to avoid clipping e.g. HOCON scopes, or URL anchors, etc.
                                // we have to retrim if we cut, or else we'll retain any whitespace that came before the comment.
                                String cut = cut_point > 0 ? trimmed.substring(0,cut_point).trim() : trimmed;

                                tmp.add(cut);
                            }
                        }
                        out.addAll( tmp );
                    }
                    catch (Exception e)
                    { delayedLogItemsOut.add(new DelayedLogItem(Level.WARNING, "An Exception occurred while trying to load an expected property config resources list from '" + resource + "'.", e)); }
                    finally
                    {
                        try { if ( br != null ) br.close(); } //ensures closure of nested raw stream as well
                        catch (IOException e)
                            { delayedLogItemsOut.add( new DelayedLogItem( Level.WARNING, "An IOException occurred while closing BufferedReader from resource path '" + MLOG_CONFIG_RESOURCE_PATHS_RESOURCE + "'.", e ) ); }
                    }
                }
            }
        }
        catch (Exception e)
        { delayedLogItemsOut.add(new DelayedLogItem(Level.WARNING, "An unexpected Exception occurred while trying to load property config resources from '" + MLOG_CONFIG_RESOURCE_PATHS_RESOURCE + "'.", e)); }

        return out;
    }

    static synchronized void markMLogReinitialized()
    {
        classLogger = MLog.getLogger(MLogConfig.class);
    }

    static synchronized List<String> getResourcePathsTxtPaths()
    { return resourcePathsTxtPaths; }

    private synchronized static MLogger getClassLogger()
    { return classLogger; }

    private synchronized static boolean isMLogInitialized()
    { return getClassLogger() != null; }

    private static String[] mlogDefaults()
    { return new String[] {STANDARD_CFG_DEFAULT_MCHANGE_LOG,STANDARD_CFG_MCHANGE_COMMONS,STANDARD_CFG_MCHANGE_LOG}; }

    private static String[] mlogPreempts()
    { return new String[] {"hocon:/reference,/application,/","/",SealedSystemProperties.NOTIONAL_RESOURCE_PATH}; }

    private static String[] mlogUnified()
    {
        return new String[] {
            STANDARD_CFG_DEFAULT_MCHANGE_LOG,
            STANDARD_CFG_MCHANGE_COMMONS,
            STANDARD_CFG_MCHANGE_LOG,
            "hocon:/reference,/application,/",
            "/",
            SealedSystemProperties.NOTIONAL_RESOURCE_PATH
        };
    }

    // we want to distinguish between several cases
    // - MLog is the first caller from its static initializer (the usual first call)
    // - A client is calling AFTER MLog has initialized (supported client use)
    // - A client is calling BEFORE MLog initialization is completed. Unexpected but harmless as long as we don't provoke logging.
    // - MLog is a caller after some other party called this (fine, but we have to respect freeze)
    //
    // After our reorg, mlog is the only caller. MLog can still be initialized or not,
    // and can still be calling from the initializer or not.
    //
    // In any case, it is now impossible, absent reflective workarounds or some such.
    // All of MLogConfig's synchronized methods are now accessed via MLog, so callers get MLog's lock first, MLogConfig second.
    synchronized static void refresh( MultiPropertiesConfig[] backstops, String backstopsDescription, MultiPropertiesConfig[] overrides, String overridesDescription, boolean fromMLogInit )
    {
	List<DelayedLogItem> bli = new ArrayList<>();

        // we don't touch SealedSystemProperties before mlog is initialized, because it logs.
        // mlog gets initialized before System properties seal, so (unsealed)
        // load before mlogInitialized is as safe as later sealed loads
	boolean mlogInitialized = isMLogInitialized();

        // in this block, we CAN access logging, because we know MLog and related classes have already been initialized
        // we make our decision under the old, potentially sunsetting config, not the proposed new one
        if (mlogInitialized)
        {
            if (freezeConfigProperty == null)
                freezeConfigProperty = new SealedSystemPropertiesBooleanProperty(FREEZE_CONFIG_KEY, true, false, false);

            MLogger logger = getClassLogger(); // guaranteed not null, once mlog is initialized

            if (fromMLogInit && logger.isLoggable(MLevel.SEVERE))
                logger.log(MLevel.SEVERE,
                           "Internal Error in logging library: It should be impossible for MLog to have already marked initialized when it's also in its class init! " +
                           "[mlogInitialized -> " + mlogInitialized + "; fromMLogInit -> " + fromMLogInit + "]");


            boolean configFrozen = freezeConfigProperty.getValue(config, logger);
            if (configFrozen)
            {
                Throwable debugStackTrace;
                if (refreshDebugWarned)
                    debugStackTrace = null;
                else
                {
                    debugStackTrace = new Exception("DEBUG STACK TRACE, attempted refresh of frozen log config (" + FREEZE_CONFIG_KEY + "=true)");
                    refreshDebugWarned = true;
                }
                if (logger.isLoggable(MLevel.WARNING))
                    logger.log(MLevel.WARNING,
                               "An attempt to refresh MLog library config will be ignored, because the configuration has been frozen (" + FREEZE_CONFIG_KEY + "=true)",
                               debugStackTrace );

                return;
            }
        }
        // else we fall through and load config, while being careful not to touch any logging

        MultiPropertiesConfig tmpConfig;

        List<String> configuredPaths = resourcePathsFromMLogConfigResourcePathsTxt(bli);
        resourcePathsTxtPaths = configuredPaths == null ? null : Collections.unmodifiableList(configuredPaths);

        if (resourcePathsTxtPaths != null && resourcePathsTxtPaths.size() > 0)
        {
            // however a user might have configured, put sealed system properties then live system properties at the top of the precedence hierarchy
            String[] resources = SealedSystemProperties.capCurrentThenSealed(resourcePathsTxtPaths.toArray(new String[resourcePathsTxtPaths.size()]));

            int indexOfDefaultMchangeLog = -1;
            int indexOfMchangeCommons    = -1;
            int indexOfMchangeLog        = -1;

            for (int i = 0, len = resources.length; i < len; ++i)
            {
                // note latest index found will win, which is what we want
                if (STANDARD_CFG_DEFAULT_MCHANGE_LOG.equals(resources[i])) indexOfDefaultMchangeLog = i;
                else if (STANDARD_CFG_MCHANGE_COMMONS.equals(resources[i])) indexOfMchangeCommons = i;
                else if (STANDARD_CFG_MCHANGE_LOG.equals(resources[i])) indexOfMchangeLog = i;
            }

            if (indexOfDefaultMchangeLog < 0)
                bli.add( new DelayedLogItem(DelayedLogItem.Level.WARNING,
                                             "Standard mlog config file '" + STANDARD_CFG_DEFAULT_MCHANGE_LOG +
                                             "' not found in the configured mlog path defined by " + MLOG_CONFIG_RESOURCE_PATHS_RESOURCE +
                                             ". We recommend this config source be included.") );
            if (indexOfMchangeCommons < 0)
                bli.add( new DelayedLogItem(DelayedLogItem.Level.WARNING,
                                             "Standard mlog config file '" + STANDARD_CFG_MCHANGE_COMMONS +
                                             "' not found in the configured mlog path defined by " + MLOG_CONFIG_RESOURCE_PATHS_RESOURCE +
                                             ". We recommend this config source be included.") );
            if (indexOfMchangeLog < 0)
                bli.add( new DelayedLogItem(DelayedLogItem.Level.WARNING,
                                             "Standard mlog config file '" + STANDARD_CFG_MCHANGE_LOG +
                                             "' not found in the configured mlog path defined by " + MLOG_CONFIG_RESOURCE_PATHS_RESOURCE +
                                             ". We recommend this config source be included.") );

            boolean canonicalOrder = true;
            int previousIndex = -1;
            for ( int index : new int[] { indexOfDefaultMchangeLog, indexOfMchangeCommons, indexOfMchangeLog } )
                if ( index >= 0 )
                {
                    if ( index < previousIndex ) canonicalOrder = false;
                    previousIndex = index;
                }

            if (! canonicalOrder )
                bli.add( new DelayedLogItem(DelayedLogItem.Level.WARNING,
                                             "Ordering of standard mlog config files is non standard. The ordering " + Arrays.toString(new String[] {STANDARD_CFG_DEFAULT_MCHANGE_LOG, STANDARD_CFG_MCHANGE_COMMONS, STANDARD_CFG_MCHANGE_LOG} ) +
                                             " is recommended.") );

            // note that it's important that we read the config uncached here, because we call this from MLog's class init,
            // and the cached pathway potentially hits a logger, which might lead to reentrancy for which we are not prepared
            // or deadlocks
            tmpConfig = MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig(null, resources, bli);
        }
        else if (MConfig.WithTraditionalDefaultSources.traditionalPathFileSourcedPathsFound(bli))
        {
            bli.add(new DelayedLogItem(DelayedLogItem.Level.WARNING,
                                       "At least one of the following resources " + MConfig.WithTraditionalDefaultSources.traditionalPathFileResourcesSearched() +
                                       " has been found, so mlog will configure itself using the traditional mechanism that honored these files. " +
                                       "However, mlog is transitioning from this mechanism, which is fragile and obscure. The supported way to customize configuration is " +
                                       "to refresh a now hardcoded configuration source list with overrides and then call MLog.refreshConfig(...) with overrides."));

            // note that if backstops or overrides are non-null, we cap later, after the overrides, if we can cap
            String[] defaults = mlogDefaults();
            String[] preempts = mlogPreempts();

            // note that it's important that we read the config uncached here, because we call this from MLog's class init,
            // and the cached pathway potentially hits a logger, which might lead to reentrancy for which we are not prepared
            // or deadlocks
            tmpConfig = MConfig.WithTraditionalDefaultSources.readUncachedResourceConfig( defaults, preempts, bli );
        }
        else
        {
            // note that if backstops or overrides are non-null, we cap later, after the overrides, if we can cap
            String[] resources = mlogUnified();

            // note that it's important that we read the config uncached here, because we call this from MLog's class init,
            // and the cached pathway potentially hits a logger, which might lead to reentrancy for which we are not prepared
            // or deadlocks
            tmpConfig = MConfig.AsProvidedIgnoreVetoed.readUncachedResourceConfig(null, resources, bli);
        }

	if ( backstops != null || overrides != null )
	{
            MultiPropertiesConfig[] nullsafeBackstops = backstops == null ? EMPTY_MPC_ARRAY : backstops;
            MultiPropertiesConfig[] nullsafeOverrides = overrides == null ? EMPTY_MPC_ARRAY : overrides;
            int blen = nullsafeBackstops.length;
	    int olen = nullsafeOverrides.length;
	    MultiPropertiesConfig[] combineMe = new MultiPropertiesConfig[ blen + 1 + olen];
            System.arraycopy(nullsafeBackstops,0,combineMe,0,blen);
	    combineMe[blen] = tmpConfig;
            System.arraycopy(nullsafeOverrides,0,combineMe,blen+1,olen);
	    config = SealedSystemProperties.initCapCurrentThenSealAndCombine(combineMe,bli);
            bli.addAll( config.getDelayedLogItems() );
            StringBuilder sb = new StringBuilder();
            sb.append(fromMLogInit ? "Loaded " : "Refreshed ");
            sb.append("MLog library log configuration");
            if (blen > 0)
            {
                sb.append(", with backstops specified");
                if (backstopsDescription != null)
                    sb.append(" -- " + backstopsDescription);
            }
            if (olen > 0)
            {
                sb.append(", with overrides specified");
                if (overridesDescription != null)
                    sb.append(" -- " + overridesDescription);
            }
            sb.append(".");
	    bli.add( new DelayedLogItem( DelayedLogItem.Level.INFO, sb.toString() ) );
	}
	else
	{
	    if ( !fromMLogInit )
		bli.add( new DelayedLogItem( DelayedLogItem.Level.INFO, "Refreshed MLog library log configuration, without backstops or overrides.") );
	    config = tmpConfig;
	}
        bli.add(new DelayedLogItem(DelayedLogItem.Level.INFO,
                                   "mlog config is now drawn from the following configuration sources: " +
                                   Arrays.toString(config.getPropertiesResourcePaths()) +
                                   ". Later paths shadow earlier paths where values conflict."));

        if (bootstrapLogItems == null)
            bootstrapLogItems = bli;
        else
            bootstrapLogItems.addAll(bli);
    }

    // should be called only from static synchronized methods
    private static void ensureDelayedDumpToLogger()
    {
	try
	{
	    if ( delayedDumpToLogger == null )
	    {
		Class<?> mConfigClass = Class.forName( "com.mchange.v2.cfg.MConfig" );
		Class<?> delayedLogItemClass = Class.forName( "com.mchange.v2.cfg.DelayedLogItem" );
		delayedDumpToLogger = mConfigClass.getMethod("dumpToLogger", new Class<?>[] { List.class, MLogger.class } );
	    }
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

    synchronized static String getPropertyOnlyIfAvailable( String key )
    {
	return (config != null ? config.getProperty( key ) : null);
    }

    synchronized static void logDelayedItems( MLogger logger )
    {
        //ensureLoad();
        ensureDelayedDumpToLogger();

        if ( bootstrapLogItems != null ) // we only want to log them once, we don't want to retain their Throwables
        {
            try { delayedDumpToLogger.invoke( null, new Object[] { bootstrapLogItems, logger } ); }
            catch ( Exception e )
            {
                // bad, bad, shouldn't happen
                e.printStackTrace();
                throw new Error(e);
            }
            bootstrapLogItems = null;
        }
    }

    synchronized static String dump()
    { return String.valueOf(config); }

    private MLogConfig()
    {}
}
